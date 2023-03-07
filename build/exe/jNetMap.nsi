; jNetMap.jar
;
; This script is based on example1.nsi, but it remember the directory, 
; has uninstall support and (optionally) installs start menu shortcuts.
;
; It will install jNetMap.jar into a directory that the user selects,

;--------------------------------
Unicode True
; The name of the installer
Name "jNetMap"
; General
VIProductVersion ;set by script
VIAddVersionKey ProductName "jNetMap"
VIAddVersionKey InternalName "jNetMap"
VIAddVersionKey CompanyName "rakudave"
VIAddVersionKey LegalCopyright "GPL 3+"
VIAddVersionKey FileDescription ""
VIAddVersionKey FileVersion ;set by script

; The file to write
OutFile "jNetMap.exe"

; The default installation directory
InstallDir $PROGRAMFILES\jNetMap

; Registry key to check for directory (so if you install again, it will 
; overwrite the old one automatically)
InstallDirRegKey HKLM "Software\jNetMap" "Install_Dir"

; Request application privileges for Windows Vista
RequestExecutionLevel admin

;--------------------------------

; Pages

Page components
Page directory
Page instfiles

UninstPage uninstConfirm
UninstPage instfiles

;--------------------------------

; The stuff to install
Section "jNetMap (required)"

  SectionIn RO
  
  ; Set output path to the installation directory.
  SetOutPath $INSTDIR
  
  ; Put file there
  File "jNetMap.jar"
  File "jNetMap.bat"
  File "jnetmap.ico"

  ; Write the installation path into the registry
  WriteRegStr HKLM SOFTWARE\jNetMap "Install_Dir" "$INSTDIR"
  
  ; Write the uninstall keys for Windows
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\jNetMap" "DisplayName" "jNetMap"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\jNetMap" "UninstallString" '"$INSTDIR\uninstall.exe"'
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\jNetMap" "NoModify" 1
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\jNetMap" "NoRepair" 1
  WriteUninstaller "uninstall.exe"
  
SectionEnd

; Optional section (can be disabled by the user)
Section "Start Menu Shortcuts"

  CreateDirectory "$SMPROGRAMS\jNetMap"
  CreateShortCut "$SMPROGRAMS\jNetMap\Uninstall jNetMap.lnk" "$INSTDIR\uninstall.exe" "" "$INSTDIR\uninstall.exe" 0
  CreateShortCut "$SMPROGRAMS\jNetMap\jNetMap.lnk" "$INSTDIR\jNetMap.bat" "" "$INSTDIR\jnetmap.ico" 0 "SW_SHOWMINIMIZED" "" "graphical network monitoring"
  
SectionEnd

;--------------------------------

; Uninstaller

Section "Uninstall"
  
  ; Remove registry keys
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\jNetMap"
  DeleteRegKey HKLM SOFTWARE\jNetMap

  ; Remove files and uninstaller
  Delete $INSTDIR\jNetMap.jar
  Delete $INSTDIR\jNetMap.bat
  Delete $INSTDIR\jnetmap.ico
  Delete $INSTDIR\uninstall.exe

  ; Remove shortcuts, if any
  Delete "$SMPROGRAMS\jNetMap\*.*"

  ; Remove directories used
  RMDir "$SMPROGRAMS\jNetMap"
  RMDir "$INSTDIR"

SectionEnd

