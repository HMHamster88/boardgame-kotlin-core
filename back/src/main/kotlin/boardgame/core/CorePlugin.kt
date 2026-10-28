package boardgame.core

import boardgame.api.CoreModule
import boardgame.api.GameCore
import boardgame.api.HostServices

class CorePlugin : CoreModule {
    override val version: String get() = BuildInfo.version

    override fun start(host: HostServices): GameCore = CoreImpl(host)
}
