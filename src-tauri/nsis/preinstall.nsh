!macro NSIS_HOOK_PREINSTALL
  DetailPrint "Closing RiftForge if running..."
  nsExec::ExecToLog 'taskkill /F /IM "RiftForge.exe" /T'
  nsExec::ExecToLog 'taskkill /F /IM "java.exe" /T'
  Sleep 1500
!macroend
