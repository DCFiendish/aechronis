# Brewery resource pack

Every brewery item and station part uses the item model `aechronis:brewery/<name>`, i.e. the item
definition `assets/aechronis/items/brewery/<name>.json`. The files shipped here are placeholders
that point at vanilla models. To replace one, add your model under
`assets/aechronis/models/brewery/<name>.json` and point the item definition at
`aechronis:brewery/<name>`. Ids never change, so code does not need touching.

## Items

| id | used for | placeholder |
|---|---|---|
| grapes, barley, honeycomb | crop ingredients | sweet berries, wheat, honeycomb |
| mint, pennyroyal, thyme | herbs (not grown) | kelp, dried kelp, sugar cane |
| grape_cutting, barley_seeds | planting (part B) | stick, wheat seeds |
| barley_meal | grain mill output | sugar |
| jug, jug_filled | clay jug, 4 measures | flower pot, potion |
| pitcher, pitcher_filled | oinochoe, 2 measures | glass bottle, honey bottle |
| amphora, amphora_filled | amphora, 16 measures | bucket, water bucket |
| test_bench_kit | admin demo station | crafting table |
| wine_press_kit, grain_mill_kit | part C stations | piston, grindstone |
| fermenter_kit, mixing_krater_kit, cellar_rack_kit | part D stations | barrel, cauldron, bookshelf |
| wine, kykeon, hydromel, zythos | beverages (one cup) | xp bottle, mushroom stew, honey bottle, rabbit stew |

## Station part models

Station parts are item displays. The model's origin is the part pivot: centre the model on the
point it rotates or moves around (displays are rendered centred on the model's 8,8,8). Station
local axes: +Z is the front (towards the player who placed it), +Y up; offsets in code are in
blocks from the bottom centre of the station's block.

| id | pivot |
|---|---|
| test_bench | centre of the bench, 0.5 above the floor |

Parts C and D add their part models here (new files only) together with a row in this table, e.g.
`wine_press_beam` (pivot at the hinge), `wine_press_basin`, `grain_mill_stone` (pivot at the axle),
`fermenter_pithos`, `fermenter_lid`, `mixing_krater`, `cellar_rack`.
