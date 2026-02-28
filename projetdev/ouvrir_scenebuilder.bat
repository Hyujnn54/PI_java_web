@echo off
set "SCENE_BUILDER=C:\Users\rayan\AppData\Local\SceneBuilder\SceneBuilder.exe"

if "%~1"=="" (
    echo Opening default file: login.fxml
    "%SCENE_BUILDER%" "src\main\resources\GUI\login.fxml"
) else (
    echo Opening: %~1
    "%SCENE_BUILDER%" "%~1"
)
