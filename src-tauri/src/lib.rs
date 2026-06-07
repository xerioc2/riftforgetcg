use std::sync::Mutex;
use tauri::Manager;

struct ServerProcess(Mutex<Option<std::process::Child>>);

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .setup(|app| {
            #[cfg(not(debug_assertions))]
            {
                let resource_dir = app.path().resource_dir()
                    .expect("resource dir not found");
                let java = resource_dir.join("jre").join("bin")
                    .join(if cfg!(windows) { "java.exe" } else { "java" });
                let jar = resource_dir.join("riftforge-server.jar");

                let child = std::process::Command::new(&java)
                    .args(["-jar", jar.to_str().unwrap()])
                    .spawn()
                    .expect("failed to start server");

                app.manage(ServerProcess(Mutex::new(Some(child))));
            }

            if let Some(win) = app.get_webview_window("main") {
                let _ = win.show();
            }

            Ok(())
        })
        .on_window_event(|window, event| {
            if let tauri::WindowEvent::Destroyed = event {
                if let Some(state) = window.app_handle().try_state::<ServerProcess>() {
                    if let Ok(mut guard) = state.0.lock() {
                        if let Some(mut child) = guard.take() {
                            let _ = child.kill();
                        }
                    }
                }
            }
        })
        .run(tauri::generate_context!())
        .expect("error running RiftForge");
}
