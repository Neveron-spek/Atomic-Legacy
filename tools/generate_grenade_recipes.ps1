param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

$recipeRoot = Join-Path $ProjectRoot 'src/main/resources/data/atomiclegacy/recipe'

function ItemIngredient([string]$id) {
    [ordered]@{ item = $id }
}

function TagIngredient([string]$tag) {
    [ordered]@{ tag = $tag }
}

function FoundryIngredient([string]$item, [int]$materialId, [string]$material) {
    [ordered]@{
        type = 'neoforge:components'
        items = @($item)
        components = [ordered]@{
            'minecraft:custom_data' = [ordered]@{
                material_id = $materialId
                material = $material
            }
        }
    }
}

function VariantResult([string]$id, [int]$count, [string]$variant, [int]$modelData) {
    $result = [ordered]@{
        id = $id
        components = [ordered]@{
            'minecraft:custom_data' = [ordered]@{ variant = $variant }
            'minecraft:custom_model_data' = $modelData
        }
    }
    if ($count -ne 1) {
        $result.count = $count
    }
    return $result
}

function WriteShaped([string]$name, [string[]]$pattern, [System.Collections.IDictionary]$key, [System.Collections.IDictionary]$result) {
    $recipe = [ordered]@{
        type = 'minecraft:crafting_shaped'
        category = 'misc'
        pattern = $pattern
        key = $key
        result = $result
    }
    $recipe | ConvertTo-Json -Depth 12 | Set-Content -Encoding utf8 (Join-Path $recipeRoot "$name.json")
}

function WriteCustomAssembly() {
    [ordered]@{ type = 'atomiclegacy:universal_grenade' } |
        ConvertTo-Json -Depth 4 |
        Set-Content -Encoding utf8 (Join-Path $recipeRoot 'universal_grenade.json')
}

$steelBolt = FoundryIngredient 'atomiclegacy:bolt' 30 'steel'
$steelShell = FoundryIngredient 'atomiclegacy:shell' 30 'steel'
$weaponSteelShell = FoundryIngredient 'atomiclegacy:shell' 50 'weaponsteel'
$aluminiumPlate = FoundryIngredient 'atomiclegacy:plate_cast' 1300 'aluminium'
$weaponSteelPlate = FoundryIngredient 'atomiclegacy:plate_cast' 50 'weaponsteel'
$combineSteelPlate = FoundryIngredient 'atomiclegacy:plate_cast' 39 'combine_steel'

# Exact 1.7.10 WeaponRecipes.java:260-284 mappings.  The absent triplex recipe
# is intentional: the legacy source did not register one.
WriteShaped 'grenade_shell_frag' @('B', 'P', 'S') ([ordered]@{
    B = $steelBolt; P = $aluminiumPlate; S = $steelShell
}) (VariantResult 'atomiclegacy:grenade_shell' 4 'frag' 0)
WriteShaped 'grenade_shell_stick' @('S', 'B', 'W') ([ordered]@{
    S = $steelShell; B = $steelBolt; W = TagIngredient 'minecraft:planks'
}) (VariantResult 'atomiclegacy:grenade_shell' 4 'stick' 1)
WriteShaped 'grenade_shell_tech' @('C', 'M', 'S') ([ordered]@{
    C = ItemIngredient 'atomiclegacy:circuit_basic'; M = ItemIngredient 'atomiclegacy:mechanism_special'; S = $weaponSteelShell
}) (VariantResult 'atomiclegacy:grenade_shell' 4 'tech' 2)
WriteShaped 'grenade_shell_nuke' @(' S ', 'CMC', ' S ') ([ordered]@{
    C = ItemIngredient 'atomiclegacy:circuit_advanced'; M = ItemIngredient 'atomiclegacy:mechanism_special'; S = $weaponSteelShell
}) (VariantResult 'atomiclegacy:grenade_shell' 2 'nuke' 3)

WriteShaped 'grenade_fuze_s3' @('S', 'F') ([ordered]@{
    S = $steelBolt; F = ItemIngredient 'atomiclegacy:safety_fuse'
}) (VariantResult 'atomiclegacy:grenade_fuze' 4 's3' 0)
WriteShaped 'grenade_fuze_s7' @('S', 'F', 'F') ([ordered]@{
    S = $steelBolt; F = ItemIngredient 'atomiclegacy:safety_fuse'
}) (VariantResult 'atomiclegacy:grenade_fuze' 4 's7' 1)
WriteShaped 'grenade_fuze_s15' @(' S ', ' F ', 'FFF') ([ordered]@{
    S = $steelBolt; F = ItemIngredient 'atomiclegacy:safety_fuse'
}) (VariantResult 'atomiclegacy:grenade_fuze' 4 's15' 2)
WriteShaped 'grenade_fuze_impact' @('C', 'S', 'F') ([ordered]@{
    C = TagIngredient 'c:dusts/smokeless'; S = $steelBolt; F = ItemIngredient 'atomiclegacy:safety_fuse'
}) (VariantResult 'atomiclegacy:grenade_fuze' 4 'impact' 3)
WriteShaped 'grenade_fuze_airburst' @('C', 'S', 'F') ([ordered]@{
    C = ItemIngredient 'atomiclegacy:circuit_vacuum_tube'; S = $steelBolt; F = ItemIngredient 'atomiclegacy:safety_fuse'
}) (VariantResult 'atomiclegacy:grenade_fuze' 4 'airburst' 4)

$polymerPlate = ItemIngredient 'atomiclegacy:plate_polymer'
WriteShaped 'grenade_filling_powder' @('F', 'I', 'F') ([ordered]@{
    F = ItemIngredient 'minecraft:gunpowder'; I = $polymerPlate
}) (VariantResult 'atomiclegacy:grenade_filling' 4 'powder' 0)
WriteShaped 'grenade_filling_he' @('F', 'I', 'F') ([ordered]@{
    F = ItemIngredient 'atomiclegacy:ball_dynamite'; I = $polymerPlate
}) (VariantResult 'atomiclegacy:grenade_filling' 4 'he' 1)
WriteShaped 'grenade_filling_demo' @('F', 'I', 'F') ([ordered]@{
    F = TagIngredient 'c:ingots/high_explosive'; I = $polymerPlate
}) (VariantResult 'atomiclegacy:grenade_filling' 4 'demo' 2)
WriteShaped 'grenade_filling_inc' @('F', 'I', 'F') ([ordered]@{
    F = ItemIngredient 'atomiclegacy:powder_fire'; I = $polymerPlate
}) (VariantResult 'atomiclegacy:grenade_filling' 4 'inc' 3)
WriteShaped 'grenade_filling_wp' @('F', 'I', 'F') ([ordered]@{
    F = ItemIngredient 'atomiclegacy:ingot_phosphorus'; I = $polymerPlate
}) (VariantResult 'atomiclegacy:grenade_filling' 4 'wp' 4)
WriteShaped 'grenade_filling_cluster' @('F', 'I', 'F') ([ordered]@{
    F = ItemIngredient 'atomiclegacy:pellet_cluster'; I = $polymerPlate
}) (VariantResult 'atomiclegacy:grenade_filling' 4 'cluster' 5)
WriteShaped 'grenade_filling_cluster_heavy' @('F', 'I', 'F') ([ordered]@{
    F = ItemIngredient 'atomiclegacy:pellet_cluster'; I = $weaponSteelPlate
}) (VariantResult 'atomiclegacy:grenade_filling' 1 'cluster_heavy' 9)
WriteShaped 'grenade_filling_emp' @(' C ', 'KWK') ([ordered]@{
    C = ItemIngredient 'atomiclegacy:circuit_basic'; K = ItemIngredient 'atomiclegacy:coil_gold'; W = $weaponSteelPlate
}) (VariantResult 'atomiclegacy:grenade_filling' 4 'emp' 6)
WriteShaped 'grenade_filling_plasma' @(' C ', 'KWK') ([ordered]@{
    C = ItemIngredient 'atomiclegacy:circuit_capacitor_board'; K = ItemIngredient 'atomiclegacy:cell_tritium'; W = $weaponSteelPlate
}) (VariantResult 'atomiclegacy:grenade_filling' 4 'plasma' 7)
WriteShaped 'grenade_filling_laser' @(' C ', 'KWK') ([ordered]@{
    C = ItemIngredient 'atomiclegacy:circuit_atomic_clock'; K = ItemIngredient 'atomiclegacy:crystal_redstone'; W = $weaponSteelPlate
}) (VariantResult 'atomiclegacy:grenade_filling' 4 'laser' 8)
WriteShaped 'grenade_filling_nuclear' @(' T ', 'CPC', ' T ') ([ordered]@{
    T = ItemIngredient 'atomiclegacy:ball_tatb'; C = $weaponSteelPlate; P = TagIngredient 'c:nuggets/pu239'
}) (VariantResult 'atomiclegacy:grenade_filling' 1 'nuclear' 10)
WriteShaped 'grenade_filling_nuclear_demo' @('TPT', 'CPC', 'TPT') ([ordered]@{
    T = ItemIngredient 'atomiclegacy:ball_tatb'; C = ItemIngredient 'atomiclegacy:neutron_reflector'; P = TagIngredient 'c:nuggets/pu239'
}) (VariantResult 'atomiclegacy:grenade_filling' 1 'nuclear_demo' 11)
WriteShaped 'grenade_filling_schrab' @('BCB', 'TST', 'BCB') ([ordered]@{
    B = $combineSteelPlate; C = ItemIngredient 'atomiclegacy:circuit_controller'; T = ItemIngredient 'atomiclegacy:ball_tatb'; S = ItemIngredient 'atomiclegacy:cell_sas3'
}) (VariantResult 'atomiclegacy:grenade_filling' 1 'schrab' 12)

WriteShaped 'grenade_extra_glue' @(' P ', 'PSP', ' P ') ([ordered]@{
    P = ItemIngredient 'minecraft:paper'; S = ItemIngredient 'minecraft:slime_ball'
}) (VariantResult 'atomiclegacy:grenade_extra' 1 'glue' 0)
WriteShaped 'grenade_extra_proxy_fuze' @('C', 'F') ([ordered]@{
    C = ItemIngredient 'atomiclegacy:circuit_chip'; F = ItemIngredient 'atomiclegacy:safety_fuse'
}) (VariantResult 'atomiclegacy:grenade_extra' 1 'proxy_fuze' 1)
WriteShaped 'grenade_extra_frag_sleeve' @('BBB', ' T ', 'BBB') ([ordered]@{
    B = $steelBolt; T = ItemIngredient 'atomiclegacy:ducttape'
}) (VariantResult 'atomiclegacy:grenade_extra' 1 'frag_sleeve' 2)

WriteCustomAssembly
