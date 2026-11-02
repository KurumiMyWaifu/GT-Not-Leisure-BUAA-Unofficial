package com.science.gtnl.common.crafting.quantum.core.localization;

public enum PlayerMessages {

    CraftingNoPlan,
    CraftingNoNetOutput,
    CraftingTooLong,
    CraftingMissingIngredient,
    CraftingAmountOverflow,
    CraftingPlanUnbalanced,
    CraftingReplayLimit,
    CraftingRecipeStalled,
    CraftingShortageResolutionLimit;

    public String getTranslationKey() {
        return "gtnl.quantum_crafting.message." + name();
    }
}
