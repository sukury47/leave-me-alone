package com.github.sukury47.leavemealone

import com.github.sukury47.leavemealone.views.IView
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage

class ScreenController(var primaryStage: Stage) {
    private var showingScreen: Screen? = null
    private val fxmlPathsByKey = mapOf(Screen.Key.ROOT to "/views/root.fxml")

    init {
        primaryStage.title = "let's suck tax together"
    }

    fun show(key: Screen.Key, showForcibly: Boolean = true) {
        if (showingScreen != null && showingScreen!!.key != key) {
            showingScreen!!.destroy()
        } else {
            showingScreen = Screen(key, fxmlPathsByKey[key]!!)
            showingScreen?.let { screen ->
                primaryStage.scene = screen.scene
                primaryStage.onCloseRequest = EventHandler {
                    screen.destroy()
                }
                if (!primaryStage.isShowing) {
                    primaryStage.show()
                }
            }
        }
    }

    class Screen(val key: Key, private val fxmlPath: String) {
        private lateinit var view: IView
        val scene: Scene by lazy {
            val loader = FXMLLoader()
            val layout =loader.load<Parent>(this.javaClass.getResourceAsStream(fxmlPath))
            view = loader.getController()
            Scene(layout)
        }

        fun destroy() {
            view.onDestroy()
        }

        enum class Key {
            ROOT
        }
    }
}