package com.github.sukury47.leavemealone

import com.github.sukury47.leavemealone.viewmodels.VMRoot
import javafx.stage.Stage
import org.koin.dsl.module

val appModule = module {
    single { (stage: Stage) -> ScreenController(primaryStage = stage) }
    factory { VMRoot() }
}