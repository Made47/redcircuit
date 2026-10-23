// RedCircuit - przykłady dla CraftTweaker (1.21.1). Skopiuj do scripts/.
// Typ receptury: redcircuit:alloying - dodawanie przez JSON (działa bez dodatkowej integracji).

<recipetype:redcircuit:alloying>.addJsonRecipe("example_alloy", {
    ingredients: [
        {tag: "c:ingots/iron", count: 1},
        {item: "minecraft:coal", count: 2}
    ],
    result: {id: "minecraft:netherite_scrap", count: 1},
    cooktime: 400
});

// Usunięcie receptury po nazwie:
// <recipetype:redcircuit:alloying>.removeByName("redcircuit:alloying_brass_ingot");

// Usunięcie po wyniku:
// <recipetype:redcircuit:alloying>.remove(<item:redcircuit:signal_alloy_ingot>);
