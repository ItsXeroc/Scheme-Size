var mod = Vars.mods.getMod("scheme-size")
var get = (pkg) => null

if (mod != null && mod.loader != null && !Vars.headless) {
    get = (pkg) => mod.loader.loadClass(pkg).newInstance()
}

const SchemeMain = mod == null ? null : mod.main
const SchemeVars = get("scheme.SchemeVars")
const SchemeUpdater = get("scheme.SchemeUpdater")
const ServerIntegration = get("scheme.ServerIntegration")
const ModedSchematics = get("scheme.moded.ModedSchematics")
