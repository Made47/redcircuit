// RedCircuit - przykłady dla KubeJS (Minecraft 26.x). Skopiuj do kubejs/server_scripts/.
// Typ receptury: redcircuit:alloying (piec do stopów i elektryczny piec do stopów).
// Składniki: lista {ingredient, count} (ingredient: 'id' albo '#tag'), do 9 pozycji; wynik: {id, count}; cooktime w tickach (domyślnie 200).

ServerEvents.recipes(event => {
  // Dodanie: stal z żelaza i węgla
  event.custom({
    type: 'redcircuit:alloying',
    ingredients: [
      { ingredient: '#c:ingots/iron', count: 1 },
      { ingredient: 'minecraft:coal', count: 2 }
    ],
    result: { id: 'minecraft:netherite_scrap', count: 1 },
    cooktime: 400
  }).id('mypack:alloying/example')

  // Usunięcie receptury z moda (po identyfikatorze)
  // event.remove({ id: 'redcircuit:alloying_brass_ingot' })

  // Usunięcie wszystkich receptur pieca do stopów dających dany przedmiot
  // event.remove({ type: 'redcircuit:alloying', output: 'redcircuit:signal_alloy_ingot' })
})
