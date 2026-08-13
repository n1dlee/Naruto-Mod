package com.sekwah.narutomod.abilities;

import com.sekwah.narutomod.capabilities.CooldownTickEvent;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.registries.NarutoRegistries;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

public abstract class Ability {

    public Ability() {
    }

    public enum ActivationType {
        INSTANT,
        TOGGLE,
        CHANNELED
    }

    /**
     * For now the combo to register the ability with. May be overrideable in the future.
     * @return
     */
    public long defaultCombo() {
        return -1;
    }

    /**
     * When returning different activation types make sure you have the correct methods implemented otherwise certain things may not happen
     * see {@link Toggled} and {@link Channeled}
     * @return
     */
    public abstract ActivationType activationType();

    /**
     * Also tell the player why they can't cast the ability.
     *
     * Handle the activation message in perform.
     *
     * In channeled abilities, this will be triggered every tick. And the charge amount will be increased by 1 each tick.
     *
     * If this fails and is above 0 for charge amount the last successful will call perform.
     *
     * If channeled and chargeAmount is -1, it will either be a minCast (if enabled) or the stop packet is
     * received at the same time. If this returns false then perform will not be called.
     *
     * @param player
     * @param ninjaData
     * @param chargeAmount
     * @return if the jutsu cost was able to be fufilled. If this is true then perform will be triggered.
     */
    public abstract boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount);

    /**
     * Do not overwrite this, use {@link Ability#handleCost(Player, INinjaData, int)}
     *
     * This is just to allow code to call the cost without the charge amount.
     * @param player
     * @param ninjaData
     */
    public boolean handleCost(Player player, INinjaData ninjaData) {
        return handleCost(player, ninjaData, 0);
    }

    /**
     * If the ability should say its status in chat
     * @return
     */
    public boolean logInChat() {
        return true;
    }

    // --- Phase 15: Nature Release ---

    /**
     * Element id this jutsu belongs to ("fire", "water", "earth", "wind", "lightning"),
     * or null for non-elemental techniques and kekkei genkai. Elemental jutsu are gated
     * centrally by unlocked element + mastery level (see checkElementRequirement) and
     * feed element XP on every successful cast.
     */
    public String element() {
        return null;
    }

    /**
     * Element mastery level required to cast (only checked when element() != null).
     */
    public int elementLevelRequired() {
        return 1;
    }

    /**
     * Second nature this jutsu also demands, or null for an ordinary single-element
     * technique. This is what makes kekkei genkai possible: Ice is not its own nature you
     * can awaken, it is what a ninja who has trained BOTH Water and Wind can do with them.
     * The bloodline is expressed as "you must own and have trained two natures at once",
     * which the element-slot cap already makes a real investment.
     */
    public String secondaryElement() {
        return null;
    }

    /** Mastery level required in {@link #secondaryElement()}, when one is set. */
    public int secondaryElementLevelRequired() {
        return 1;
    }

    /**
     * Element XP granted per successful cast of this jutsu.
     */
    public float elementXpReward() {
        return 15f;
    }

    /**
     * How long this jutsu's cast stance holds, in ticks, before PlayerAnimHandler blends the
     * player back to their normal animation.
     *
     * This used to be a flat 8 for every INSTANT ability in the mod, which made a summoning
     * and a substitution take exactly as long to perform. Overriding it is how a technique
     * gets weight: a slow, deliberate jutsu should hold the stance long enough to read as
     * effort, and an escape should be over before the player registers it happened.
     *
     * Only the hold is set here - the ease in and ease out on either side are added by
     * PoseBlender and are not counted in this number.
     */
    public int castPoseTicks() {
        return 8;
    }

    /**
     * Phase 15 C: scroll-taught jutsu must be learned before casting. Checked centrally
     * from the activation/channel packet handlers together with the element gate.
     */
    /**
     * True when the Sharingan has this exact technique stored as a stolen copy. A copy is
     * a one-shot pass that waives both the scroll and the nature requirement — you are
     * mimicking what you saw, not drawing on training you never had.
     */
    public boolean isCopiedBySharingan(INinjaData ninjaData) {
        var resourceKey = NarutoRegistries.ABILITIES.getResourceKey(this);
        return resourceKey.isPresent()
                && resourceKey.get().location().getPath().equals(ninjaData.getCopiedJutsu());
    }

    public boolean checkLearnedRequirement(Player player, INinjaData ninjaData) {
        var resourceKey = NarutoRegistries.ABILITIES.getResourceKey(this);
        if (resourceKey.isEmpty()) {
            return true;
        }
        String path = resourceKey.get().location().getPath();
        if (!JutsuScrolls.requiresScroll(path) || ninjaData.isJutsuLearned(path)
                || path.equals(ninjaData.getCopiedJutsu())) {
            return true;
        }
        player.displayClientMessage(Component.translatable("jutsu.fail.notlearned",
                Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                .withStyle(ChatFormatting.RED), true);
        return false;
    }

    /**
     * Central elemental gate: the caster must have this jutsu's element unlocked and
     * trained to the required mastery level. Non-elemental jutsu always pass.
     */
    public boolean checkElementRequirement(Player player, INinjaData ninjaData) {
        String element = this.element();
        if (element == null || this.isCopiedBySharingan(ninjaData)) {
            return true;
        }
        if (!checkOneElement(player, ninjaData, element, this.elementLevelRequired())) {
            return false;
        }
        String secondary = this.secondaryElement();
        return secondary == null
                || checkOneElement(player, ninjaData, secondary, this.secondaryElementLevelRequired());
    }

    /**
     * Pure (no messaging, no side effects) version of the element gate, for callers that
     * need to know "could this be cast right now" without attempting a cast - JutsuScreen
     * recolours every row every frame and must never spam chat. Mirrors
     * {@link #hasEyeAccess(INinjaData)}.
     */
    public boolean hasElementAccess(INinjaData ninjaData) {
        String element = this.element();
        if (element == null) {
            return true;
        }
        if (!ninjaData.isElementUnlocked(element)
                || ninjaData.getElementLevel(element) < this.elementLevelRequired()) {
            return false;
        }
        String secondary = this.secondaryElement();
        return secondary == null
                || (ninjaData.isElementUnlocked(secondary)
                        && ninjaData.getElementLevel(secondary) >= this.secondaryElementLevelRequired());
    }

    /**
     * One nature's half of the gate: owned, and trained far enough. Split out so the
     * kekkei genkai check reports the SECOND element by name when that is the one the
     * caster is short on - being told "you need Water" when your Wind is what is lacking
     * would be actively misleading.
     */
    private boolean checkOneElement(Player player, INinjaData ninjaData, String element, int levelRequired) {
        Component elementName = Component.translatable("element.narutomod." + element).withStyle(ChatFormatting.YELLOW);
        if (!ninjaData.isElementUnlocked(element)) {
            player.displayClientMessage(Component.translatable("jutsu.fail.element.locked",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW),
                    elementName).withStyle(ChatFormatting.RED), true);
            return false;
        }
        int level = ninjaData.getElementLevel(element);
        if (level < levelRequired) {
            player.displayClientMessage(Component.translatable("jutsu.fail.element.level",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW),
                    elementName,
                    Component.literal(String.valueOf(levelRequired)).withStyle(ChatFormatting.YELLOW),
                    Component.literal(String.valueOf(level)).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.RED), true);
            return false;
        }
        return true;
    }

    /**
     * Clan whose bloodline this technique belongs to, or null when anyone may learn it.
     *
     * Clan-locked techniques used to hide their gate inside handleCost(), which spends
     * chakra as a side effect and so could never be safely asked "would this work?" - that
     * is why the jutsu screen has to paint them a noncommittal grey. Declaring the clan
     * here instead makes the gate checkable without casting, exactly like requiredEye().
     */
    public String requiredClan() {
        return null;
    }

    /** Pure, side-effect-free clan check, safe to call from the GUI every frame. */
    public boolean hasClanAccess(INinjaData ninjaData) {
        String clan = this.requiredClan();
        return clan == null || clan.equals(ninjaData.getClanId());
    }

    public boolean checkClanRequirement(Player player, INinjaData ninjaData) {
        if (this.hasClanAccess(ninjaData)) {
            return true;
        }
        player.displayClientMessage(Component.translatable("jutsu.fail.clan",
                Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW),
                Component.translatable("naruto.clan." + this.requiredClan()).withStyle(ChatFormatting.YELLOW))
                .withStyle(ChatFormatting.RED), true);
        return false;
    }

    // --- Phase 16: Dojutsu gating ---

    /**
     * Eye required to cast this technique, or null for eye-agnostic jutsu. One of:
     * "sharingan_tomoe1".."sharingan_tomoe3", "sharingan_ms", "sharingan_ems",
     * "byakugan", "rinnegan", "rinnegan_path:&lt;id&gt;", "rinne_sharingan".
     * Checked centrally from the activation/channel packet handlers, next to the
     * element and scroll gates.
     */
    public String requiredEye() {
        return null;
    }

    /**
     * Mangekyo form that owns this signature technique ("itachi", "sasuke", "madara",
     * "shisui", "obito"), or null when any Mangekyo may cast it. A player commands a
     * form either by awakening it or by taking it from the boss who owned it.
     */
    public String requiredEyeForm() {
        return null;
    }

    /**
     * Pure (no messaging, no side effects) version of the dojutsu check — same rules as
     * {@link #checkEyeRequirement}, minus the chat message on failure. Used wherever code
     * needs to know "could this player cast this eye-gated jutsu right now" without
     * actually attempting the cast, e.g. JutsuScreen's live status colouring, which runs
     * every frame and must never spam chat.
     */
    private String eyeMessageKey(String eye) {
        return eye.startsWith("rinnegan_path:") ? "rinnegan_path" : eye;
    }

    private boolean hasEyeOwned(INinjaData ninjaData) {
        String eye = this.requiredEye();
        if (eye == null) {
            return true;
        }
        if (eye.startsWith("sharingan_tomoe")) {
            int tomoe = eye.charAt(eye.length() - 1) - '0';
            return ninjaData.getSharinganTomoe() >= tomoe || ninjaData.isMangekyoAwakened();
        }
        if (eye.startsWith("rinnegan_path:")) {
            String path = eye.substring("rinnegan_path:".length());
            return ninjaData.isRinneganAwakened()
                    && (ninjaData.isRinneganPathUnlocked(path) || ninjaData.isRinneSharinganAwakened());
        }
        return switch (eye) {
            case "sharingan_ms" -> ninjaData.isMangekyoAwakened();
            case "sharingan_ems" -> ninjaData.isEternalMangekyoAwakened();
            case "byakugan" -> ninjaData.getByakuganLevel() >= 1;
            case "rinnegan" -> ninjaData.isRinneganAwakened() || ninjaData.isRinneSharinganAwakened();
            case "rinne_sharingan" -> ninjaData.isRinneSharinganAwakened();
            default -> true;
        };
    }

    private boolean hasEyeForm(INinjaData ninjaData) {
        String form = this.requiredEyeForm();
        return form == null || ninjaData.hasSignatureForm(form);
    }

    /**
     * Pure (no messaging, no side effects) version of the dojutsu check — same rules as
     * {@link #checkEyeRequirement}, minus the chat message on failure. Used wherever code
     * needs to know "could this player cast this eye-gated jutsu right now" without
     * actually attempting the cast, e.g. JutsuScreen's live status colouring, which runs
     * every frame and must never spam chat.
     */
    public boolean hasEyeAccess(INinjaData ninjaData) {
        return this.hasEyeOwned(ninjaData) && this.hasEyeForm(ninjaData);
    }

    /**
     * Central dojutsu gate, called from the activation/channel packet handlers. Same rule
     * set as {@link #hasEyeAccess}, but also messages the player on failure.
     */
    public boolean checkEyeRequirement(Player player, INinjaData ninjaData) {
        if (this.requiredEye() == null) {
            return true;
        }
        if (!this.hasEyeOwned(ninjaData)) {
            player.displayClientMessage(Component.translatable(
                    "jutsu.fail.eye." + this.eyeMessageKey(this.requiredEye()),
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (!this.hasEyeForm(ninjaData)) {
            player.displayClientMessage(Component.translatable("jutsu.fail.eye.form",
                    Component.translatable(this.getTranslationKey(ninjaData)).withStyle(ChatFormatting.YELLOW),
                    Component.translatable("mangekyo.form." + this.requiredEyeForm()).withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        return true;
    }

    /**
     * Awards XP after a successful cast — call sites live in the ability
     * activation/channel packet handlers so every jutsu path funnels through one place.
     * Elemental jutsu train their element; every cast also grants a token amount of
     * rank XP (practice) — the real rank progression comes from landed hits and kills
     * (see PlayerEvents), since chakra spend no longer drips XP.
     */
    public void grantCastXp(INinjaData ninjaData) {
        ninjaData.addChakraXp(2f);
        String element = this.element();
        if (element != null) {
            ninjaData.addElementXp(element, this.elementXpReward());
        }
    }

    /**
     *
     * @return sound to play, if null no sound should be played
     */
    public SoundEvent castingSound() {
       return NarutoSounds.JUTSU_CAST.get();
    }

    /**
     * Sound to play when an ability fails to cast.
     * @return
     */
    public SoundEvent castingFailSound() {
        return NarutoSounds.JUTSU_FAIL.get();
    }

    public String getTranslationKey(INinjaData ninjaData) {
        var resourceKey = NarutoRegistries.ABILITIES.getResourceKey(this);
        return this.getTranslationKey(ninjaData, 0);
    }

    /**
     * If something should be added to the end of the translation string to modify what is said.
     *
     * No longer can replace based on registry name as those are not longer provided by the forge entries.
     * @param ticksActive
     * @return
     */
    public String getTranslationKey(INinjaData ninjaData, int ticksActive) {
        var resourceKey = NarutoRegistries.ABILITIES.getResourceKey(this);
        if(resourceKey.isPresent()) {
            return resourceKey.get().location().toString();
        }
        return "";
    }

    /**
     *
     * @param player the entity casting the jutsu. Will just be players for now. Though may be entity in the future.
     * @param ninjaData
     * @param ticksActive
     */
    public abstract void performServer(Player player, INinjaData ninjaData, int ticksActive);

    /**
     * Do not overwrite this, use {@link Ability#performServer(Player, INinjaData, int)}
     * @param player
     * @param ninjaData
     */
    public void performServer(Player player, INinjaData ninjaData) {
        this.performServer(player, ninjaData, 0);
    }

    public interface Toggled {

        /**
         *
         * For if abilities need to do anything client side (atm its only triggered with toggle to stop a spam of packets)
         *
         * @param player
         * @param ninjaData
         */
        void performToggleClient(Player player, INinjaData ninjaData);

    }

    public interface ToggleStartCheck {
        boolean canStartToggle(Player player, INinjaData ninjaData);
    }

    /**
     * Whether releasing a channel after this many ticks counts as a real cast where
     * cooldowns are concerned. True by default.
     *
     * Abilities with a minimum wind-up (Kirin) return false for a release that came too
     * early: the technique never happened, so charging it should not lock you out of it
     * for the next forty-five seconds.
     */
    public boolean channelCommittedAt(int ticksChanneled) {
        return true;
    }

    /**
     * Channeled and charged abilities are handled the same way as there is so much overlap.
     */
    public interface Channeled {

        /**
         * If the jutsu is not channeled at all, should the jutsu activate? Stops abilities like the channeling activating for a single tick.
         *
         * @return
         */
        default boolean canActivateBelowMinCharge() {
            return true;
        }

        /**
         * If to use the charged translation strings instead of charged.
         *
         * This alters if the chat should show the ability as stopped or activated or charged and cast.
         *
         * @return
         */
        default boolean useChargedMessages() {
            return false;
        }

        /**
         * In case of other use cases where you don't want the messages. for custom states e.g. substitution.
         * @return
         */
        default boolean hideChannelMessages() {
            return false;
        }

        /**
         * Call every tick handleCost passes on server side.
         *
         * This is the main behavior that seperates a "channeled" ability from a "charged" ability as the behaviors are the same.
         *  @param player
         * @param ninjaData
         * @param ticksChanneled
         */
        default void handleChannelling(Player player, INinjaData ninjaData, int ticksChanneled) {}
    }

    public interface HandleEnded {
        /**
         * This will trigger on the player when they are no longer able to cast the ability or when they re-active
         * the ability to cancel it.
         *
         * For now the toggle abilities will return 0, though will return how long they lasted in the future
         * e.g. if a jutsu should hurt you more if it was left on for longer.
         *
         * This will be triggered on ActivationType.TOGGLE and ActivationType.CHARGED.
         *
         * @param player
         * @param ninjaData
         */
        void handleAbilityEnded(Player player, INinjaData ninjaData, int ticksActive);
    }

    /**
     * Interface class to append a cooldown to a jutsu.
     */
    public interface Cooldown
    {

        /**
         * Method to  get the cooldown  value.
         * @return the cooldown specified.
         */
        int getCooldown();

        /**
         * Check if a cooldown exists for this jutus.
         * @param player - the player the jutsu is being cased from.
         * @param ninjaData - the ninjaData capability attached to the player.
         * @param translationKey - the translation key for the jutsu / unique name.
         * @return  return true if a cooldown exists or false if no cooldown exists.
         */
        default boolean checkCooldown(Player player, INinjaData ninjaData, String translationKey) {
            if  (getCooldown() > 0 && ninjaData.getCooldownEvents().containsKey(translationKey)) {
                player.displayClientMessage(Component.translatable("jutsu.fail.cooldown",
                        Component.translatable(translationKey).withStyle(ChatFormatting.YELLOW),
                        Component.literal(String.valueOf((int) Math.ceil(ninjaData.getCooldownEvents().get(translationKey).ticks / 20f))).withStyle(ChatFormatting.YELLOW)
                ), true);
                return  true;
            }
            return  false;
        }

        /**
         * Registers that a cooldown should exist for this jutus and sets a cooldown timer in the NinjaData
         * @param ninjaData - the ninjaData capability attached to the player.
         * @param translationKey - the translation key for the jutsu / unique name.
         */
        default void registerCooldown(INinjaData ninjaData, String translationKey) {
            if (getCooldown() > 0)  {
                ninjaData.getCooldownEvents().put(translationKey, new CooldownTickEvent(getCooldown()));
            }
        }
    }
}
