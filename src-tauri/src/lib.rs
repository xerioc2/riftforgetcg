#[cfg(not(debug_assertions))]
use std::fs::OpenOptions;
#[cfg(not(debug_assertions))]
use std::io::Write;
#[cfg(not(debug_assertions))]
use std::path::{Path, PathBuf};
#[cfg(not(debug_assertions))]
use std::process::{Command, Stdio};
use std::sync::Mutex;
use tauri::Manager;

#[cfg(all(windows, not(debug_assertions)))]
use std::os::windows::process::CommandExt;

struct ServerProcess(Mutex<Option<std::process::Child>>);

#[cfg(not(debug_assertions))]
fn write_startup_log(message: &str) {
    let path = std::env::temp_dir().join("riftforge-startup.log");
    if let Ok(mut file) = OpenOptions::new().create(true).append(true).open(path) {
        let _ = writeln!(file, "{message}");
    }
}

#[cfg(not(debug_assertions))]
fn java_compatible_path(path: &Path) -> PathBuf {
    #[cfg(windows)]
    {
        let text = path.to_string_lossy();
        return PathBuf::from(text.strip_prefix(r"\\?\").unwrap_or(&text));
    }

    #[cfg(not(windows))]
    path.to_path_buf()
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .setup(|app| {
            #[cfg(not(debug_assertions))]
            {
                let resource_dir = app.path().resource_dir()?;
                let java = java_compatible_path(
                    &resource_dir.join("jre").join("bin")
                        .join(if cfg!(windows) { "java.exe" } else { "java" }),
                );
                let jar = java_compatible_path(&resource_dir.join("riftforge-server.jar"));

                write_startup_log(&format!(
                    "Starting server: java={} jar={}",
                    java.display(),
                    jar.display()
                ));

                let mut command = Command::new(&java);
                command
                    .args(["-jar", jar.to_str().unwrap()])
                    .stdout(Stdio::null())
                    .stderr(Stdio::null());

                #[cfg(windows)]
                command.creation_flags(0x08000000);

                let child = command.spawn().map_err(|error| {
                    write_startup_log(&format!("Failed to start server: {error}"));
                    error
                })?;
                write_startup_log(&format!("Server process started with PID {}", child.id()));

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
