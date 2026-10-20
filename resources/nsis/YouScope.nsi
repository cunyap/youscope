;NSIS Installer for YouScope
;Written by Moritz Lang and
;Andreas P. Cuny 
;---------------------
;--- Configuration ---
;---------------------

; YouScope is distributed as a 64-bit application.
; The installer contains its own JDK 21 runtime.
!define WIN64


;--------------------------------
;Set to moder design/UI
!include "MUI2.nsh"
!include "LogicLib.nsh"
!include "Sections.nsh"
!include "x64.nsh"
!include "StrFunc.nsh"
${StrRep}

;--------------------------------
;General
;Name and file
Name "YouScope"
OutFile "YouScope_64bit_Installer.exe"

;Default installation folder
InstallDir "$PROGRAMFILES64\YouScope2"

;Get installation folder from registry if available
InstallDirRegKey HKCU "Software\YouScope64" ""

;Request application privileges for Windows Vista/7
RequestExecutionLevel admin

;--------------------------------
;Variables
Var StartMenuFolder
; Comma separated list of the selected Python-based plugins, filled in .onInstSuccess
; and handed to setup_graalpy_venv.ps1 (-Plugins) so only the needed environments are built.
Var PythonPlugins

;--------
; Config
BrandingText "YouScope - The Microscope Control Software" 


;--------------------------------
;Interface Settings
!define MUI_ABORTWARNING
!define MUI_ICON "YouScope.ico"
!define MUI_UNICON "YouScope.ico"

;--------------------------------
;Pages
!insertmacro MUI_PAGE_WELCOME

!define MUI_LICENSEPAGE_RADIOBUTTONS
!insertmacro MUI_PAGE_LICENSE "LICENSE"

!insertmacro MUI_PAGE_COMPONENTS

!insertmacro MUI_PAGE_DIRECTORY
  
;Start Menu Folder Page Configuration
!define MUI_STARTMENUPAGE_REGISTRY_ROOT "HKCU" 
!define MUI_STARTMENUPAGE_REGISTRY_KEY "Software\YouScope2" 
!define MUI_STARTMENUPAGE_REGISTRY_VALUENAME "StartMenuFolder"
!insertmacro MUI_PAGE_STARTMENU Application $StartMenuFolder
  
!insertmacro MUI_PAGE_INSTFILES

!define MUI_FINISHPAGE_NOREBOOTSUPPORT
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

;--------------------------------
;Languages
!insertmacro MUI_LANGUAGE "English"

;--------------------------------
; Download-at-install helper (requires the NSIS "Inetc" plugin on the build
; machine installed in build.yml). Used for any JAR that is not committed
; to youscope/libs because of a redistribution-incompatible license or because it is too large to ship.
;
;   ${DownloadLib} "https://host/path/foo.jar" "foo.jar"
;
; Downloads into $INSTDIR\lib. On failure it warns but continues, so an offline
; install still yields a working core program.
!define BASE_LIB_URL "https://github.com/cunyap/youscope/tree/master/libs"

!macro DownloadLibMacro Url FileName
    SetOutPath "$INSTDIR\lib"
    IfFileExists "$INSTDIR\lib\${FileName}" +6 0
    DetailPrint "Downloading ${FileName} ..."
    inetc::get /CAPTION "Downloading ${FileName}" /RESUME "" \
        "${Url}" "$INSTDIR\lib\${FileName}" /END
    Pop $0
    StrCmp $0 "OK" +2 0
    MessageBox MB_OK|MB_ICONEXCLAMATION \
        "Could not download ${FileName} ($0).$\nSome optional features may be disabled.$\nYou can place the file in $INSTDIR\lib manually later."
!macroend
!define DownloadLib "!insertmacro DownloadLibMacro"


;--------------------------------
; Install types
InstType "Full"
InstType "Minimal"
InstType "Server"
InstType "Client"

; Splash screen
Function .onInit
  ; 64-bit application: use the native registry view so that the uninstall
  ; entries land in HKLM\SOFTWARE and not in WOW6432Node.
  SetRegView 64

  SetOutPath $TEMP
  File /oname=spltmp.bmp "splash.bmp"

  advsplash::show 1000 600 400 -1 $TEMP\spltmp

  Pop $0 ; $0 has '1' if the user closed the splash screen early,
         ; '0' if everything closed normally, and '-1' if some error occurred.

  Delete $TEMP\spltmp.bmp
FunctionEnd

Function VCRedistNeeded

FunctionEnd  

Function un.onInit
  SetRegView 64
FunctionEnd


;--------------------------------
;Main Program
Section "-Main Program" SecMain
	SectionIn 1 2 3 4
  
	SetOutPath "$INSTDIR"
	
	; We install the MSVC Runtime if not yet installed
	File "msvc\vcredist_x64.exe"
	
	ExecWait "$INSTDIR\vcredist_x64.exe /install /passive"
	
	Delete "$INSTDIR\vcredist_x64.exe"
			
	FILE LICENSE
	FILE YouScope64.exe

	; Python environment setup (run by .onInstSuccess for the selected plugins, or manually later)
	FILE /nonfatal "setup_graalpy_venv.bat"
	FILE /nonfatal "setup_graalpy_venv.ps1"

    ; Private JDK 21. YouScope does not require a system Java installation.
    SetOutPath "$INSTDIR\runtime"
    FILE /r "runtime\*"
    SetOutPath "$INSTDIR"

    ; Sanity check: the bundled JDK 21 must contain a JVM, otherwise
    ; YouScope64.exe cannot start.
    IfFileExists "$INSTDIR\runtime\bin\server\jvm.dll" +2 0
        MessageBox MB_OK|MB_ICONSTOP "The bundled Java 21 runtime is incomplete ($INSTDIR\runtime\bin\server\jvm.dll is missing).$\nYouScope will not start. Please rebuild the installer with a full JDK/JRE 21 in the 'runtime' folder."

    ; The JVM options (--add-opens, --enable-native-access, java.library.path, ...) are
    ; embedded in YouScope64.exe by Gradle (createExe.jvmOptions). Do not write a
    ; YouScope64.l4j.ini: Launch4j appends its options unquoted, so an install path with a
    ; space (C:\Program Files\...) splits an option and Java exits with code 1.
    ; Remove an .ini left behind by an earlier installer build.
    Delete "$INSTDIR\YouScope64.l4j.ini"
	
	AccessControl::GrantOnFile "$INSTDIR" "(BU)" "FullAccess" 
	Pop $0

	SetOutPath "$INSTDIR\lib"
	FILE "lib\*"

	;Store installation folder
    WriteRegStr HKCU "Software\YouScope64" "" $INSTDIR

    WriteUninstaller "$INSTDIR\Uninstall.exe"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\YouScope" "DisplayName" "YouScope - Microscope Control"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\YouScope" "UninstallString" '"$INSTDIR\Uninstall.exe"'
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\YouScope" "QuietUninstallString" '"$INSTDIR\Uninstall.exe" /S'
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\YouScope" "InstallLocation" "$INSTDIR"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\YouScope" "DisplayIcon" "$INSTDIR\YouScope64.exe"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\YouScope" "Publisher" "YouScope.org"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\YouScope" "DisplayVersion" "2.4.0"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\YouScope" "URLInfoAbout" "http://www.youscope.org"
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\YouScope" "NoModify" 1
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\YouScope" "NoRepair" 1

	; Add start menu entry
	!insertmacro MUI_STARTMENU_WRITE_BEGIN Application
		;Create shortcuts
		SetShellVarContext all
		CreateDirectory "$SMPROGRAMS\$StartMenuFolder"
		CreateShortCut "$SMPROGRAMS\$StartMenuFolder\YouScope64.lnk" "$INSTDIR\YouScope64.exe"
        CreateShortCut "$SMPROGRAMS\$StartMenuFolder\Uninstall.lnk" "$INSTDIR\Uninstall.exe"
	!insertmacro MUI_STARTMENU_WRITE_END

SectionEnd

;--------------------------------
;Client
Section "!Client" SecClient
	SetOutPath "$INSTDIR\lib"
	SectionIn 1 2 4
  
	;Files:
	FILE "lib\youscope-client.jar"
SectionEnd

;--------------------------------
; Server
Section "!Server" SecServer
	SectionIn 1 2 3
  
	;Files:
	SetOutPath "$INSTDIR"
	FILE "YSConfig_demo.cfg"

	; Default client settings (FlatDark skin, demo configuration). Written only when the
	; file does not exist yet, so an upgrade never overwrites the user's own settings.
	; Generated here because it contains the install path,
	; which must be escaped as in a Java .properties file (C\:\\Program Files\\...).
	IfFileExists "$INSTDIR\youscope_properties.prop" ys_props_done 0
		${StrRep} $R0 "$INSTDIR" "\" "\\"
		${StrRep} $R0 "$R0" ":" "\:"
		FileOpen $0 "$INSTDIR\youscope_properties.prop" w
		FileWrite $0 "#YouScope Settings$\r$\n"
		FileWrite $0 "YouScope.configFileLast0=$R0\\YSConfig_demo.cfg$\r$\n"
		FileWrite $0 "YouScope.general.startupType=CLIENT_SERVER$\r$\n"
		FileWrite $0 "YouScope.server.forceConnectionConfiguration=false$\r$\n"
		FileWrite $0 "YouScope.server.lastPort=1237$\r$\n"
		FileWrite $0 "YouScope.server.lastURL=127.000.000.001$\r$\n"
		FileWrite $0 "YouScope.server.microscopeConnectionType=YouScope.StandAlone$\r$\n"
		FileWrite $0 "YouScope.skin=YouScope.Skins.FlatDark$\r$\n"
		FileClose $0
	ys_props_done:
	SetOutPath "$INSTDIR\lib"
	FILE "lib\youscope-server.jar"
	SetOutPath "$INSTDIR\plugins"
	File "plugins\youscope-microscopeaccess.jar"
SectionEnd

;--------------------------------
;Drivers
Section "!Device Drivers" SecDeviceDrivers
	SectionIn 1 2 3
  
		SetOutPath "$INSTDIR\drivers64"
		FILE /r "drivers64\*"
		SetOutPath "$INSTDIR\drivers64\nemesys"
		RegDLL "$INSTDIR\drivers64\nemesys\NemesysDotCom.dll"
SectionEnd

;--------------------------------
;Microplate Types
SectionGroup "Microplate Types" SecMicroplateTypes
	Section "ANSI/SBS microplates" SecAnsiSbsMicroplates
  		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 2 3 4
  
  		;Files:
  		FILE "plugins\youscope-ansi-sbs-microplates.jar"  
	SectionEnd
	
	Section "Custom microplates" SecCustomMicroplates
  		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 3 4
  
  		;Files:
  		FILE "plugins\youscope-custom-microplates.jar"  
	SectionEnd
	
	Section "Multiwell TC microplates" SecMultiwellTC
  		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 3 4
  
  		;Files:
  		FILE "plugins\youscope-bd-bioscience-multiwell-tc-microplates.jar"  
	SectionEnd

	Section "Custom arbitrary microplate editor" SecArbitraryMicroplateEditor
  		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 3 4
  
  		;Files:
  		FILE "plugins\youscope-custom-arbitrary-microplate-editor.jar"  
	SectionEnd
SectionGroupEnd

;--------------------------------
;Measurement Types
SectionGroup "Measurement Types" SecMeasurementTypes

	Section "Simple Measurement" SecSimpleMeasurement
  		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 2 3 4
  
  		;Files:
  		FILE "plugins\youscope-simple-measurement.jar"  
	SectionEnd


	Section "Microplate Measurement" SecMicroplateMeasurement
  		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 2 3 4
  
  		;Files:
  		FILE "plugins\youscope-microplate-measurement.jar"  
	SectionEnd

	Section "Continous Measurement" SecContinousMeasurement
  		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 2 3 4
  
  		;Files:
  		FILE "plugins\youscope-continous-imaging.jar"  
	SectionEnd

	Section "Composed Imaging Measurement" SecComposedImagingMeasurement
  		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 3 4
  
  		;Files:
  		FILE "plugins\youscope-composed-imaging.jar"  
	SectionEnd

	Section "Continuation Measurement" SecContinuationMeasurement
  		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 3 4
  
  		;Files:
  		FILE "plugins\youscope-continuation-measurement.jar"  
	SectionEnd

	Section "Task Measurement" SecTaskMeasurement
  		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 2 3 4
  
  		;Files:
  		FILE "plugins\youscope-task-measurement.jar"  
	SectionEnd

	Section "User-Control Measurement" SecUserControlMeasurement
  		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 3 4
  
  		;Files:
  		FILE "plugins\youscope-user-control-measurement.jar"  
	SectionEnd

SectionGroupEnd

SectionGroup "Measurement Result Consumers" SecResultConsumers
	;Section "OpenBIS Uploader" SecOpenBIS
  	;	SectionIn 1 3 4
  	;
  	;	;Files:
	;	SetOutPath "$INSTDIR\plugins"
  	;	FILE "plugins\youscope-openbis.jar"  
	;	SetOutPath "$INSTDIR\openbis"
	;	FILE "openbis\*"  
	;	SetOutPath "$INSTDIR\openbis\.ssh"
	;	FILE "openbis\.ssh\DROP_HERE_PRIVATE_OPENSSH_KEY"  
	;SectionEnd
	Section "Folder Opener" SecOpenFolder
  		SectionIn 1 2 3 4
  
  		;Files:
		SetOutPath "$INSTDIR\plugins"
  		FILE "plugins\youscope-open-measurement-folder.jar"  
	SectionEnd
	Section "Show Measurement Information" SecShowMeasurementInfo
  		SectionIn 1 2 3 4
  
  		;Files:
		SetOutPath "$INSTDIR\plugins"
  		FILE "plugins\youscope-show-measurement-information.jar"  
	SectionEnd
SectionGroupEnd

;--------------------------------
;Job Types
SectionGroup "Job Types" SecJobTypes
	Section "Imaging Job" SecImagingJob
  		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 2 3 4
  
  		;Files:
  		FILE "plugins\youscope-imaging-job.jar"  
	SectionEnd
	
	Section "Image Subtraction Job" SecImageSubstractionJob
  		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 2 3 4
  
  		;Files:
  		FILE "plugins\youscope-image-substraction-job.jar"  
	SectionEnd

	Section "Out-Of-Focus Job" SecOutOfFocusJob
  		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 2 3 4
  
  		;Files:
  		FILE "plugins\youscope-out-of-focus-job.jar"  
	SectionEnd

	Section "Z-Slides Job" SecZSlidesJob
  		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 2 3 4
  
  		;Files:
  		FILE "plugins\youscope-z-slides.jar"  
	SectionEnd	

	Section "Scripting Job" SecScriptingJob
  		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 3 4
  
  		;Files:
  		FILE "plugins\youscope-scripting-job.jar"  
	SectionEnd	
	
	Section "Stage-Position Job" SecStagePositionJob
  		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 2 3 4
  
  		;Files:
  		FILE "plugins\youscope-change-position-job.jar"  
	SectionEnd
	
	Section "Focusing Job" SecFocusingJob
  		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 2 3 4
  
  		;Files:
  		FILE "plugins\youscope-focusing-job.jar"  
	SectionEnd

	Section "Wait Job" SecWaitJob
  		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 2 3 4
  
  		;Files:
  		FILE "plugins\youscope-wait-job.jar"  
	SectionEnd

	Section "Wait-For-Input Job" SecWaitForUser
  		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 3 4
  
  		;Files:
  		FILE "plugins\youscope-wait-for-user.jar"  
	SectionEnd

	Section "Device-Settings Job" SecDeviceSettingsJob
  		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 2 3 4
  
  		;Files:
  		FILE "plugins\youscope-device-job.jar"  
	SectionEnd	

	Section "Statistics Job" SecStatisticsJob
  		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 2 3 4
  
  		;Files:
  		FILE "plugins\youscope-statistics-job.jar"  
	SectionEnd	
	
	Section "Device-Slides Job" SecDeviceSlidesJob
  		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 3 4
  
  		;Files:
  		FILE "plugins\youscope-multi-position.jar"  
	SectionEnd	

	Section "Job Container" SecJobContainerJob
  		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 2 3 4
  
  		;Files:
  		FILE "plugins\youscope-composite-job.jar"  
	SectionEnd	
	
	Section "Oscillating Device Job" SecOscillatingDeviceJob
  		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 3 4
  
  		;Files:
  		FILE "plugins\youscope-oscillating-device-job.jar"  
	SectionEnd

	Section "Autofocus Job" SecAutofocusJob
  		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 3 4
  
  		;Files:
  		FILE "plugins\youscope-autofocus.jar"  
	SectionEnd
	Section "Life cell-detection" SecCellDetection
		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 3 4
  
  		;Files:
  		FILE "plugins\youscope-cell-detection.jar"  
	SectionEnd
	Section "CellX cell detection" SecCellX
  		SectionIn 1 3 4
  
  		;Files:
		SetOutPath "$INSTDIR\plugins"
  		FILE "plugins\youscope-cellx.jar"  
		SetOutPath "$INSTDIR\cellx"
		FILE /r "cellx\*"  
	SectionEnd
	Section "Controller" SecController
		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 3 4
  
  		;Files:
  		FILE "plugins\youscope-controller.jar"  
	SectionEnd
	Section "Custom Job" SecCustomJob
		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 3 4
  
  		;Files:
  		FILE "plugins\youscope-custom-job.jar"  
	SectionEnd
	Section "Droplet Microfluidics" SecDropletMicrofluidics
		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 3 4
  
  		;Files:
  		FILE "plugins\youscope-droplet-microfluidics.jar"  
	SectionEnd
	Section "Nemesys" SecNemesys
		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 3 4
  
  		;Files:
  		FILE "plugins\youscope-nemesys.jar"  
	SectionEnd
	;Section "Fluigent" SecFluigent
	;	SetOutPath "$INSTDIR\plugins"
  	;	SectionIn 1 3 4
  
  		;Files:
  	;	FILE "plugins\youscope-fluigent.jar"  
	;SectionEnd
	Section "Live Measurement" SecLiveMeasurement
		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 2 3 4
  
  		;Files:
  		FILE "plugins\youscope-live-modifiable-job.jar"  
	SectionEnd
	Section "Repeat Job" SecRepeatJob
		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 3 4
  
  		;Files:
  		FILE "plugins\youscope-repeat-job.jar"  
	SectionEnd
	Section "SLIM" SecSLIM
		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 3 4
  
  		;Files:
  		FILE "plugins\youscope-slim.jar"  
	SectionEnd
	Section "SLIM Control" SecSLIMControl
		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 3 4
  
  		;Files:
  		FILE "plugins\youscope-slim-control.jar"  
	SectionEnd
	Section "Share Execution" SecShareExecution
		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 3 4
  
  		;Files:
  		FILE "plugins\youscope-share-execution.jar"  
	SectionEnd
	Section "Wait since last action" SecWaitSinceLastAction
		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 3 4
  
  		;Files:
  		FILE "plugins\youscope-wait-since-last-action.jar"  
	SectionEnd
SectionGroupEnd

;--------------------------------
; Tools
SectionGroup "Tools" SecTools
	Section "YouScope LiveStream" SecLiveStream
  		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 2 4
  
  		;Files:
  		FILE "plugins\youscope-livestream.jar"  
	SectionEnd

	Section "YouScope Multi-Color Stream" SecMultiStream
  		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 4
  
  		;Files:
  		FILE "plugins\youscope-multi-color-stream.jar"  
	SectionEnd

	Section "Device Setting Manager" SecDeviceSettingManager
  		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 2 4
  
  		;Files:
  		FILE "plugins\youscope-device-setting-manager.jar"  
	SectionEnd
	
	Section "Focus Viewer" SecFocusViewer
  		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 2 4
  
  		;Files:
  		FILE "plugins\youscope-focus-viewer.jar"  
	SectionEnd


	Section "Stage and Focus Position" SecPositionControl
  		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 2 4
  
  		;Files:
  		FILE "plugins\youscope-position-control.jar"  
	SectionEnd

	Section "Measurement Viewer" SecMeasurementViewer
  		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 2 4
  
  		;Files:
  		FILE "plugins\youscope-measurement-viewer.jar"  
	SectionEnd

	Section "YouScope Scripting" SecScripting
  		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 3 4
  
  		;Files:
  		FILE "plugins\youscope-scripting.jar"  
	SectionEnd
SectionGroupEnd

;----------------------------
; Misc Functionality
SectionGroup "Misc" SecOptional
	;--------------------------------
	;Installer Matlab
	Section "Matlab" SecMatlab
	  	SetOutPath "$INSTDIR\plugins"
	  	SectionIn 1 3 4

	  	;Files:
  		FILE "plugins\youscope-matlab-scripting.jar"
	SectionEnd

	;--------------------------------
	; GraalPy / Python 3 scripting.
	; All GraalPy/Truffle runtime JARs (polyglot, truffle-*, python-*, icu4j,
	; word, collections, regex, nativeimage, js-language) are committed to
	; youscope/libs and copied into lib/ by Gradle. They ship inside the
	; installer via the "FILE lib\*" line in SecMain. No per-JAR file lines
	; are needed here. The heavy *Python packages* (numpy, cellpose, stardist,
	; torch, tensorflow, scipy) are not bundled; they are fetched at first run
	; by setup_graalpy_venv.ps1, which .onInstSuccess runs once for the selected plugins.
	Section "GraalPy Scripting (Python 3)" SecGraalPy
	  SetOutPath "$INSTDIR\plugins"
	  SectionIn 1 3 4
      FILE "plugins\youscope-graalpy-scripting.jar"


      ; Grant write access to $INSTDIR so the (non-admin) user can later create
      ; the venvs under Program Files without elevation.
      AccessControl::GrantOnFile "$INSTDIR" "(BU)" "FullAccess"
      Pop $0

	SectionEnd

	;--------------------------------
	; GraalJS / JavaScript scripting (GraalVM JS, Java interop).
	; js-language-24.1.0.jar + polyglot/truffle already ship via lib\*.
	Section "GraalJS Scripting (JavaScript)" SecGraalJs
	  SetOutPath "$INSTDIR\plugins"
	  SectionIn 1 3 4
      FILE "plugins\youscope-graaljs-scripting.jar"
	SectionEnd

	;--------------------------------
	; Cellpose deep-learning cell segmentation (uses the CPython env + torch).
	Section "Cellpose Cell Segmentation" SecCellpose
	  SetOutPath "$INSTDIR\plugins"
	  SectionIn 1 3 4
      FILE "plugins\youscope-cellpose.jar"
	SectionEnd

	;--------------------------------
	; StarDist star-convex nucleus segmentation (uses the CPython env + tensorflow).
	Section "StarDist Nucleus Segmentation" SecStarDist
	  SetOutPath "$INSTDIR\plugins"
	  SectionIn 1 3 4
      FILE "plugins\youscope-stardist.jar"
	SectionEnd

	;--------------------------------
	; Tracking, analysis and calibration plugins.
	; Jar names follow the convention plugins\youscope-<gradle-module>.jar.
	Section "Trackpy Tracker" SecTrackpy
	  SetOutPath "$INSTDIR\plugins"
	  SectionIn 1 3 4
      FILE "plugins\youscope-trackpy-tracker.jar"
	SectionEnd

	Section "BTrack Tracker" SecBTrack
	  SetOutPath "$INSTDIR\plugins"
	  SectionIn 1 3 4
      FILE "plugins\youscope-btrack-tracker.jar"
	SectionEnd

	Section "CellProfiler Quantifier" SecCellProfiler
	  SetOutPath "$INSTDIR\plugins"
	  SectionIn 1 3 4
      FILE "plugins\youscope-cellprofiler-quantifier.jar"
	SectionEnd

	Section "TrackMate Tracker" SecTrackMate
	  SetOutPath "$INSTDIR\plugins"
	  SectionIn 1 3 4
      FILE "plugins\youscope-trackmate-tracker.jar"
	SectionEnd

	Section "TIE" SecTie
	  SetOutPath "$INSTDIR\plugins"
	  SectionIn 1 3 4
      FILE "plugins\youscope-tie.jar"
	SectionEnd

	Section "FASTER" SecFaster
	  SetOutPath "$INSTDIR\plugins"
	  SectionIn 1 3 4
      FILE "plugins\youscope-faster.jar"
	SectionEnd

	Section "Argolight Calibration" SecArgolight
	  SetOutPath "$INSTDIR\plugins"
	  SectionIn 1 3 4
      FILE "plugins\youscope-argolight-calibration.jar"
	SectionEnd

	;--------------------------------
	; FAIR data export (OME-Zarr / OME-TIFF, metadata).
	; Runtime deps (zarr-java, blosc, zstd-jni, jackson, snakeyaml, minio,
	; ome-*, formats-*) are all committed to libs and ship via lib\*.
	Section "FAIR Data Export" SecFairExport
	  SetOutPath "$INSTDIR\plugins"
	  SectionIn 1 3 4
      FILE "plugins\youscope-fair-export.jar"
	SectionEnd

	;--------------------------------
	;Installer Image formats
	Section "Image Formats" SecImageFormats
		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 2 3 4
  
  		;Files:
  		FILE "plugins\jai_imageio.jar"  
	SectionEnd

	Section "Standard Save Settings" SecStandardSaveSettings
		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 2 3 4
  
  		;Files:
  		FILE "plugins\youscope-standard-save-settings.jar"  
	SectionEnd

	Section "Custom Save Settings" SecCustomSaveSettings
		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 3 4
  
  		;Files:
  		FILE "plugins\youscope-custom-save-settings.jar"  
	SectionEnd

	Section "Custom Metadata" SecCustomMetadata
		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 3 4
  
  		;Files:
  		FILE "plugins\youscope-custom-metadata.jar"  
	SectionEnd

	Section "Standard Paths" SecStandardPaths
		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 2 3 4
  
  		;Files:
  		FILE "plugins\youscope-standard-paths.jar"  
	SectionEnd

	Section "Traveling Salesman" SecTravelingSalesman
		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 3 4
  
  		;Files:
  		FILE "plugins\youscope-traveling-salesman.jar"  
	SectionEnd

	Section "Default Skin (System)" SecSystemSkin
		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 2 4
  
  		;Files:
  		FILE "plugins\youscope-system-skin.jar"  
	SectionEnd
	Section "Dark Skin" SecDarkSkin
		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 4
  
  		;Files:
  		FILE "plugins\youscope-dark-skin.jar"  
	SectionEnd
	Section "FlatLaf Skin (Modern)" SecFlatLafSkin
		SetOutPath "$INSTDIR\plugins"
  		SectionIn 1 4
  
  		;Files:
  		FILE "plugins\youscope-flatlaf-skin.jar"  
	SectionEnd
	;Section "Red Skin" SecRedSkin
	;	SetOutPath "$INSTDIR\plugins"
  	;	SectionIn 1 4
  
  	;	;Files:
  	;	FILE "plugins\youscope-red-skin.jar"  
	;SectionEnd

SectionGroupEnd

;--------------------------------
;Documentation
Section "Documentation" SecDocumentation
	SectionIn 1 4
  
	;Files:
	SetOutPath "$INSTDIR\documentation"
	FILE "documentation\documentation.pdf"  
SectionEnd

;--------------------------------
;Multi Camera
SectionGroup "Multi-Camera" SecMultiCam
	Section "Multi-Camera Job" SecParallelImagingJob
		SetOutPath "$INSTDIR\plugins"
		SectionIn 1 3 4

	  	;Files:
  		FILE "plugins\youscope-parallel-imaging-job.jar"  
	SectionEnd

	Section "Multi-Camera Stream" SecMultiCameraStream
		SetOutPath "$INSTDIR\plugins"
		SectionIn 1 4

	  	;Files:
  		FILE "plugins\youscope-multi-camera-stream.jar"  
	SectionEnd

	Section "Multi-Camera and Color Stream" SecMultiCameraAndColorStream
		SetOutPath "$INSTDIR\plugins"
		SectionIn 1 4

	  	;Files:
  		FILE "plugins\youscope-multi-camera-and-color-stream.jar"  
	SectionEnd

	Section "Multi-Camera Measurement" SecMultiCameraMeasurement
		SetOutPath "$INSTDIR\plugins"
		SectionIn 1 3 4

	  	;Files:
  		FILE "plugins\youscope-multi-camera-continous-imaging.jar"  
	SectionEnd


SectionGroupEnd

;--------------------------------
;Example Scripts
Section "Example Scripts" SecExampleScripts
	SectionIn 1 4
  
	;Files:
	SetOutPath "$INSTDIR\scripts"
	FILE /r "scripts\"  
SectionEnd


;--------------------------------
; Post-install: (re)create the Python environments and graalpy_config.txt
; for the Python-based plugins that were selected. Runs once, after all sections,
; so it sees the final selection. The script is idempotent: environments that
; already exist are reused, graalpy_config.txt is merged (manual edits are kept).
!macro AddPythonPlugin SecId Name
  SectionGetFlags ${SecId} $1
  IntOp $1 $1 & ${SF_SELECTED}
  ${If} $1 <> 0
    ${If} $PythonPlugins == ""
      StrCpy $PythonPlugins "${Name}"
    ${Else}
      StrCpy $PythonPlugins "$PythonPlugins,${Name}"
    ${EndIf}
  ${EndIf}
!macroend

Function .onInstSuccess
  StrCpy $PythonPlugins ""
  !insertmacro AddPythonPlugin ${SecGraalPy}      "graalpy"
  !insertmacro AddPythonPlugin ${SecCellpose}     "cellpose"
  !insertmacro AddPythonPlugin ${SecStarDist}     "stardist"
  !insertmacro AddPythonPlugin ${SecTrackpy}      "trackpy"
  !insertmacro AddPythonPlugin ${SecBTrack}       "btrack"
  !insertmacro AddPythonPlugin ${SecCellProfiler} "cellprofiler"
  !insertmacro AddPythonPlugin ${SecTrackMate}    "trackmate"
  !insertmacro AddPythonPlugin ${SecFairExport}   "fairexport"
  !insertmacro AddPythonPlugin ${SecFaster}       "faster"
  !insertmacro AddPythonPlugin ${SecCellDetection} "celldetection"

  ${If} $PythonPlugins == ""
    Return
  ${EndIf}
  ; Never download gigabytes in silent mode; the user can run setup_graalpy_venv.bat later.
  ${If} ${Silent}
    Return
  ${EndIf}

  MessageBox MB_YESNO|MB_ICONQUESTION \
      "The selected plugins need Python environments: $PythonPlugins$\n$\n\
This downloads roughly 2-5 GB and takes 10-40 minutes. graalpy_config.txt is updated, \
existing settings in it are kept.$\n$\n\
CellProfiler additionally needs a full JDK (17 recommended) and the Microsoft C++ Build Tools.$\n$\n\
Run the Python setup now? (Recommended and needs an internet connection.)$\n$\n\
You can also run it later (or again after adding plugins) by double-clicking:$\n$INSTDIR\setup_graalpy_venv.bat" \
      IDNO python_setup_done
  ; The installer is a 32-bit process: use the native 64-bit PowerShell (sysnative) so that
  ; Program Files paths resolve correctly. -PauseAtEnd keeps the window open so the
  ; result and any errors can be read; details are also logged to setup_graalpy_venv.log.
  ExecWait '"$WINDIR\sysnative\WindowsPowerShell\v1.0\powershell.exe" -ExecutionPolicy Bypass -NoProfile -File "$INSTDIR\setup_graalpy_venv.ps1" -Plugins "$PythonPlugins" -PauseAtEnd'
  python_setup_done:
FunctionEnd

;--------------------------------
; Uninstaller
Section "Uninstall"
    SetShellVarContext all

    ; Unregister the 64-bit Nemesys COM DLL before its files are deleted.
    IfFileExists "$INSTDIR\drivers64\nemesys\NemesysDotCom.dll" 0 +2
        ExecWait '"$WINDIR\sysnative\regsvr32.exe" /u /s "$INSTDIR\drivers64\nemesys\NemesysDotCom.dll"'

    RMDir /r "$INSTDIR\runtime"
    RMDir /r "$INSTDIR\drivers64"
    RMDir /r "$INSTDIR\lib"
    RMDir /r "$INSTDIR\plugins"
    RMDir /r "$INSTDIR\documentation"
    RMDir /r "$INSTDIR\cellx"
    RMDir /r "$INSTDIR\openbis"
	; Remove Python environments
    RMDir /r "$INSTDIR\graalpy-venv"
    RMDir /r "$INSTDIR\cpython-env"
    RMDir /r "$INSTDIR\graalpy-standalone"
    RMDir /r "$INSTDIR\uv"
    RMDir /r "$INSTDIR\cellprofiler-env"
    RMDir /r "$INSTDIR\fiji-env"

    Delete "$INSTDIR\YouScope64.exe"
    Delete "$INSTDIR\LICENSE"
    Delete "$INSTDIR\YSConfig_demo.cfg"
    Delete "$INSTDIR\youscope_properties.prop"
    Delete "$INSTDIR\YouScope64.l4j.ini"
    Delete "$INSTDIR\setup_graalpy_venv.bat"
    Delete "$INSTDIR\setup_graalpy_venv.ps1"
    Delete "$INSTDIR\graalpy_config.txt"
    Delete "$INSTDIR\graalpy_requirements.txt"
    Delete "$INSTDIR\setup_graalpy_venv.log"
    Delete "$INSTDIR\graalpy_config.txt.bak"
    Delete "$INSTDIR\launch_youscope_jdk21.bat"
    Delete "$INSTDIR\launch_youscope_jdk17.bat"
    Delete "$INSTDIR\vcredist_x64.exe"

    ; Do not recursively remove scripts or $INSTDIR: these may contain
    ; measurements, configuration, user scripts, or Python environments.
    Delete "$INSTDIR\Uninstall.exe"

    !insertmacro MUI_STARTMENU_GETFOLDER Application $StartMenuFolder
    SetShellVarContext all
    Delete "$SMPROGRAMS\$StartMenuFolder\YouScope64.lnk"
    Delete "$SMPROGRAMS\$StartMenuFolder\Uninstall.lnk"
    RMDir "$SMPROGRAMS\$StartMenuFolder"

    DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\YouScope"
    DeleteRegKey HKCU "Software\YouScope64"
    DeleteRegKey HKCU "Software\YouScope2"

    RMDir "$INSTDIR"

    MessageBox MB_OK "YouScope has been uninstalled. User-created data remaining in the installation directory was preserved."
SectionEnd

;--------------------------------
;Descriptions
	;Language strings
	LangString DESC_SecMain ${LANG_ENGLISH} "The main program. Must be installed."
	LangString DESC_SecClient ${LANG_ENGLISH} "The graphical user interface (GUI)."
	LangString DESC_SecServer ${LANG_ENGLISH} "The microscope control software."
	LangString DESC_SecDeviceDrivers ${LANG_ENGLISH} "The drivers for the microscope devices. Should always be installed with the server, except if the drivers of an installed MicroManager, version 1.4 or higher, should be used."

	LangString DESC_SecMicroplateTypes ${LANG_ENGLISH} "Types of microplate which can be screened."
	LangString DESC_SecAnsiSbsMicroplates ${LANG_ENGLISH} "96, 384, and 1536 well microplates (ANSI/SBS 1-2004 through ANSI/SBS 4-2004)."
	LangString DESC_SecCustomMicroplates ${LANG_ENGLISH} "Possibility to define own microplate types."
	LangString DESC_SecMultiwellTC ${LANG_ENGLISH} "6, 12, and 24 well microplates (BD Bioscience - Multiwell TC Plate)."

	LangString DESC_SecMeasurementTypes ${LANG_ENGLISH} "Types of measurements which can be performed by YouScope."
	LangString DESC_SecMicroplateMeasurement ${LANG_ENGLISH} "A microplate measurement screens through all wells of a microplate."
	LangString DESC_SecContinousMeasurement ${LANG_ENGLISH} "Continuously imaging one channel at one position. Including burst mode."
	LangString DESC_SecComposedImagingMeasurement ${LANG_ENGLISH} "Imaging of overlapping positions, such that the single images can later on be composed to a large one."
	LangString DESC_SecTaskMeasurement ${LANG_ENGLISH} "Running of one or more imaging protocols in parallel, e.g. to image several channels with the same or different period lengths."

	LangString DESC_SecResultConsumers ${LANG_ENGLISH} "Elements which can process or display the results of a measurement."
;	LangString DESC_SecOpenBIS ${LANG_ENGLISH} "Uploading of measurement images and meta-data to the OpenBIS screening database."
	LangString DESC_SecOpenFolder ${LANG_ENGLISH} "Simple tool to open the folder in which the measurement was saved."

	LangString DESC_SecJobTypes ${LANG_ENGLISH} "Jobs are small bricks of actions which compose an imaging protocol, and can be added to several different measurement protocols."
	LangString DESC_SecImagingJob ${LANG_ENGLISH} "Taking an image in a channel."
	LangString DESC_SecImageSubstractionJob ${LANG_ENGLISH} "Taking two out-of-focus images and divide them."
	LangString DESC_SecOutOfFocusJob ${LANG_ENGLISH} "Taking an out-of-focus image."
	LangString DESC_SecZSlidesJob ${LANG_ENGLISH} "Taking several images at different focus positions."
	LangString DESC_SecScriptingJob ${LANG_ENGLISH} "Possibility to control the microscope using scripts."
	LangString DESC_SecStagePositionJob ${LANG_ENGLISH} "Changing the relative or absolute position of the stage."
	LangString DESC_SecDeviceSettingsJob ${LANG_ENGLISH} "Changing a setting of a device."
	LangString DESC_SecDeviceSlidesJob ${LANG_ENGLISH} "Setting a device property to several values and performing the same jobs (e.g. imaging) for every value. Generalized version of z-slides."
	LangString DESC_SecJobContainerJob ${LANG_ENGLISH} "A job which simply consists of other jobs. Used to arrange jobs similar to the file system."
	LangString DESC_SecOscillatingDeviceJob ${LANG_ENGLISH} "Sending of an oscillatory signal (e.g. light) to the cells."

	LangString DESC_SecTools ${LANG_ENGLISH} "Tools to manipulate the microscope or prepare a measurement."
	LangString DESC_SecLiveStream ${LANG_ENGLISH} "The LiveStream continuously displays the current microscope image in a given channel."
	LangString DESC_SecMultiStream ${LANG_ENGLISH} "Similar to the LiveStream. Overlays the images of several channels (up to 4) in a color image."
	LangString DESC_SecPositionControl ${LANG_ENGLISH} "Possibility to display and change the position of the stage and the focus."
	LangString DESC_SecFocusViewer ${LANG_ENGLISH} "Displays the focus position."
	LangString DESC_SecMeasurementViewer ${LANG_ENGLISH} "Displays the results of a measurement."
	LangString DESC_SecScripting ${LANG_ENGLISH} "Environment to display, manipulate, run and debug scripts in various languages."
	;LangString DESC_SecYouPong ${LANG_ENGLISH} "Small game to be played by changing the stage position and focus. Experimental."
	
	LangString DESC_SecOptional ${LANG_ENGLISH} "Optional functionality."
	LangString DESC_SecMatlab ${LANG_ENGLISH} "Enabling Matlab(TM) scripting. An installation of Matlab is needed for this plugin."
	LangString DESC_SecGraalPy ${LANG_ENGLISH} "Python 3 scripting through GraalPy. YouScope includes the required JDK 21 runtime. Offers to download the Python package environment (numpy, cellpose, stardist, torch, tensorflow) on first install."
	LangString DESC_SecGraalJs ${LANG_ENGLISH} "JavaScript scripting through GraalJS, with full Java interop. Uses the bundled JDK 21 runtime; no extra download required."
	LangString DESC_SecCellpose ${LANG_ENGLISH} "Deep-learning cell segmentation (Cellpose). Requires the GraalPy Python environment and PyTorch, which are downloaded by the Python setup script."
	LangString DESC_SecStarDist ${LANG_ENGLISH} "Star-convex nucleus segmentation (StarDist). Requires the GraalPy Python environment and TensorFlow, which are downloaded by the Python setup script."
	LangString DESC_SecFairExport ${LANG_ENGLISH} "Export measurements as FAIR datasets (OME-Zarr / OME-TIFF) with rich metadata. All required Java libraries ship with the installer; the optional napari / OME-Zarr viewer tools are installed into the Python environment."
	LangString DESC_SecImageFormats ${LANG_ENGLISH} "Possibility to save images in various image formats like tiff."

	LangString DESC_SecTrackpy ${LANG_ENGLISH} "Particle/cell tracking with trackpy. Requires the GraalPy Python environment (downloaded by the Python setup script)."
	LangString DESC_SecBTrack ${LANG_ENGLISH} "Cell tracking with btrack. Requires the GraalPy Python environment (downloaded by the Python setup script)."
	LangString DESC_SecCellProfiler ${LANG_ENGLISH} "Quantification of cell features with CellProfiler. Requires the GraalPy Python environment (downloaded by the Python setup script)."
	LangString DESC_SecTrackMate ${LANG_ENGLISH} "Cell tracking with TrackMate.  Requires the GraalPy Python environment (downloaded by the Python setup script)."
	LangString DESC_SecTie ${LANG_ENGLISH} "Quantitative pahse imaging TIE plugin for YouScope."
	LangString DESC_SecFaster ${LANG_ENGLISH} "fastER segmentation plugin for YouScope."
	LangString DESC_SecArgolight ${LANG_ENGLISH} "Microscope calibration using Argolight calibration slides."
	LangString DESC_SecArbitraryMicroplateEditor ${LANG_ENGLISH} "Editor to define microplates with arbitrary well layouts."
	LangString DESC_SecContinuationMeasurement ${LANG_ENGLISH} "Measurement which continues a previously run measurement."
	LangString DESC_SecShowMeasurementInfo ${LANG_ENGLISH} "Displays information about a measurement."
	LangString DESC_SecSLIMControl ${LANG_ENGLISH} "User interface to control SLIM microscopy hardware."

	LangString DESC_SecDocumentation ${LANG_ENGLISH} "In program documentation of the YouScope tools and measurement types."
	LangString DESC_SecExampleScripts ${LANG_ENGLISH} "Script snipplets which exemplify how jobs can be programmed by using different script engines."

	LangString DESC_SecStatisticsJob ${LANG_ENGLISH} "Collects statistics of the run-time of imaging tasks and saves it as CSV."
	LangString DESC_SecMultiCameraMeasurement ${LANG_ENGLISH} "Measurement which continuously takes images with multiple cameras, either in burst mode or with fixed period. Experimental!"
	LangString DESC_SecMultiCameraAndColorStream ${LANG_ENGLISH} "Displays the current images of multiple cameras as an overlay image, in which each camera has a separate color. Experimental!"
	LangString DESC_SecDeviceSettingManager ${LANG_ENGLISH} "Allows to directly set the state of the single devices."
	LangString DESC_SecWaitJob ${LANG_ENGLISH} "Job which pauses execution either for a fixed time period, or executes sub-jobs and guarantees a minimal overall execution time. Experimental!"
	LangString DESC_SecParallelImagingJob ${LANG_ENGLISH} "Job to take images whith multiple cameras in parallel. Experimental!"
	LangString DESC_SecFocusingJob ${LANG_ENGLISH} "Job to change the focus position."
	LangString DESC_SecMultiCameraStream ${LANG_ENGLISH} "Displays the current image of multiple cameras in a grid like fashion. Experimental!"
	LangString DESC_SecSimpleMeasurement ${LANG_ENGLISH} "Measurement to execute a given protocol at a given position. Protocol can be executed more than once, with different period settings."
	LangString DESC_SecWaitForUser ${LANG_ENGLISH} "Job to display a message to the user. The execution of the protocol gets paused until user acknowledges/confirms message."
	LangString DESC_SecUserControlMeasurement ${LANG_ENGLISH} "Measurement which displays the current microscope image similar to the life-stream. When user hits a button, the currently displayed image gets saved."


	LangString DESC_SecAutofocusJob ${LANG_ENGLISH} "Job to find the focal plane by different software based autofocus algorithms." 
	LangString DESC_SecCellDetection ${LANG_ENGLISH} "Job to detect and track cells during a measurement." 
	LangString DESC_SecCellX ${LANG_ENGLISH} "Advanced cell detection algorithm. Requires cell detection job and Matlab." 
	LangString DESC_SecController ${LANG_ENGLISH} "Allows to implement feedback control algorithms." 
	LangString DESC_SecCustomJob ${LANG_ENGLISH} "Allows to define custom reusable job types." 
	LangString DESC_SecDropletMicrofluidics ${LANG_ENGLISH} "Droplet based microfluidic control algorithm." 
	;LangString DESC_SecFluigent ${LANG_ENGLISH} "User interface and job to control Fluigent microfluidic pumps." 
	LangString DESC_SecNemesys ${LANG_ENGLISH} "User interface and job to control Nemesys syringe systems." 
	LangString DESC_SecLiveMeasurement ${LANG_ENGLISH} "Job allowing to modify its sub-jobs during a measurement." 
	LangString DESC_SecRepeatJob ${LANG_ENGLISH} "Job repeating its child jobs several times."
	LangString DESC_SecSLIM ${LANG_ENGLISH} "Support for SLIM microscopy."


	LangString DESC_SecCustomMetadata ${LANG_ENGLISH} "Possibility to customize which metadata is by default saved for measurements, and which has to be saved." 
	LangString DESC_SecCustomSaveSettings ${LANG_ENGLISH} "Possibility to customize how measurement is saved to disk." 
	LangString DESC_SecStandardSaveSettings ${LANG_ENGLISH} "Standard settings how measurement is saved to disk." 
	LangString DESC_SecSystemSkin ${LANG_ENGLISH} "Default skin, in agreement to operating system look-and-feel." 
	LangString DESC_SecDarkSkin ${LANG_ENGLISH} "A dark skin for dark rooms." 
	LangString DESC_SecFlatLafSkin ${LANG_ENGLISH} "A modern, flat cross-platform look-and-feel (FlatLaf) with light and dark variants." 
	;LangString DESC_SecRedSkin ${LANG_ENGLISH} "A red skin, good for the eyes in dark rooms." 

	LangString DESC_SecTravelingSalesman ${LANG_ENGLISH} "Traveling salesman optimizers for path through microplate." 
 	LangString DESC_SecStandardPaths ${LANG_ENGLISH} "Simple standard paths through microplate." 

	LangString DESC_SecWaitSinceLastAction ${LANG_ENGLISH} "Job which guarantees that between its last execution and the current execution a certain minimal time has passed." 
	LangString DESC_SecShareExecution ${LANG_ENGLISH} "Staggering between wells and multiple positions." 

	;Assign language strings to sections
	!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
		!insertmacro MUI_DESCRIPTION_TEXT ${SecMain} $(DESC_SecMain)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecClient} $(DESC_SecClient)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecServer} $(DESC_SecServer)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecDeviceDrivers} $(DESC_SecDeviceDrivers)

		!insertmacro MUI_DESCRIPTION_TEXT ${SecMicroplateTypes} $(DESC_SecMicroplateTypes)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecAnsiSbsMicroplates} $(DESC_SecAnsiSbsMicroplates) 
		!insertmacro MUI_DESCRIPTION_TEXT ${SecCustomMicroplates} $(DESC_SecCustomMicroplates) 
		!insertmacro MUI_DESCRIPTION_TEXT ${SecMultiwellTC} $(DESC_SecMultiwellTC) 

		!insertmacro MUI_DESCRIPTION_TEXT ${SecMeasurementTypes} $(DESC_SecMeasurementTypes) 
		!insertmacro MUI_DESCRIPTION_TEXT ${SecMicroplateMeasurement} $(DESC_SecMicroplateMeasurement) 
		!insertmacro MUI_DESCRIPTION_TEXT ${SecContinousMeasurement} $(DESC_SecContinousMeasurement) 
		!insertmacro MUI_DESCRIPTION_TEXT ${SecComposedImagingMeasurement} $(DESC_SecComposedImagingMeasurement) 
		!insertmacro MUI_DESCRIPTION_TEXT ${SecTaskMeasurement} $(DESC_SecTaskMeasurement) 

		!insertmacro MUI_DESCRIPTION_TEXT ${SecResultConsumers} $(DESC_SecResultConsumers) 
		;!insertmacro MUI_DESCRIPTION_TEXT ${SecOpenBIS} $(DESC_SecOpenBIS)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecOpenFolder} $(DESC_SecOpenFolder) 

		!insertmacro MUI_DESCRIPTION_TEXT ${SecJobTypes} $(DESC_SecJobTypes) 
		!insertmacro MUI_DESCRIPTION_TEXT ${SecImagingJob} $(DESC_SecImagingJob) 
		!insertmacro MUI_DESCRIPTION_TEXT ${SecImageSubstractionJob} $(DESC_SecImageSubstractionJob) 
		!insertmacro MUI_DESCRIPTION_TEXT ${SecOutOfFocusJob} $(DESC_SecOutOfFocusJob) 
		!insertmacro MUI_DESCRIPTION_TEXT ${SecZSlidesJob} $(DESC_SecZSlidesJob) 
		!insertmacro MUI_DESCRIPTION_TEXT ${SecScriptingJob} $(DESC_SecScriptingJob) 
		!insertmacro MUI_DESCRIPTION_TEXT ${SecStagePositionJob} $(DESC_SecStagePositionJob) 
		!insertmacro MUI_DESCRIPTION_TEXT ${SecDeviceSettingsJob} $(DESC_SecDeviceSettingsJob) 
		!insertmacro MUI_DESCRIPTION_TEXT ${SecDeviceSlidesJob} $(DESC_SecDeviceSlidesJob) 
		!insertmacro MUI_DESCRIPTION_TEXT ${SecJobContainerJob} $(DESC_SecJobContainerJob) 
		!insertmacro MUI_DESCRIPTION_TEXT ${SecOscillatingDeviceJob} $(DESC_SecOscillatingDeviceJob) 

		!insertmacro MUI_DESCRIPTION_TEXT ${SecTools} $(DESC_SecTools) 
		!insertmacro MUI_DESCRIPTION_TEXT ${SecLiveStream} $(DESC_SecLiveStream) 
		!insertmacro MUI_DESCRIPTION_TEXT ${SecMultiStream} $(DESC_SecMultiStream) 
		!insertmacro MUI_DESCRIPTION_TEXT ${SecPositionControl} $(DESC_SecPositionControl) 
		!insertmacro MUI_DESCRIPTION_TEXT ${SecFocusViewer} $(DESC_SecFocusViewer) 
		!insertmacro MUI_DESCRIPTION_TEXT ${SecMeasurementViewer} $(DESC_SecMeasurementViewer) 
		!insertmacro MUI_DESCRIPTION_TEXT ${SecScripting} $(DESC_SecScripting) 
		;!insertmacro MUI_DESCRIPTION_TEXT ${SecYouPong} $(DESC_SecYouPong) 

		!insertmacro MUI_DESCRIPTION_TEXT ${SecOptional} $(DESC_SecOptional) 	
		!insertmacro MUI_DESCRIPTION_TEXT ${SecMatlab} $(DESC_SecMatlab)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecGraalPy} $(DESC_SecGraalPy)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecGraalJs} $(DESC_SecGraalJs)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecCellpose} $(DESC_SecCellpose)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecStarDist} $(DESC_SecStarDist)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecFairExport} $(DESC_SecFairExport)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecImageFormats} $(DESC_SecImageFormats)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecTrackpy} $(DESC_SecTrackpy)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecBTrack} $(DESC_SecBTrack)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecCellProfiler} $(DESC_SecCellProfiler)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecTrackMate} $(DESC_SecTrackMate)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecTie} $(DESC_SecTie)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecFaster} $(DESC_SecFaster)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecArgolight} $(DESC_SecArgolight)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecArbitraryMicroplateEditor} $(DESC_SecArbitraryMicroplateEditor)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecContinuationMeasurement} $(DESC_SecContinuationMeasurement)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecShowMeasurementInfo} $(DESC_SecShowMeasurementInfo)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecSLIMControl} $(DESC_SecSLIMControl)

		!insertmacro MUI_DESCRIPTION_TEXT ${SecDocumentation} $(DESC_SecDocumentation) 
		!insertmacro MUI_DESCRIPTION_TEXT ${SecExampleScripts} $(DESC_SecExampleScripts)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecStandardPaths} $(DESC_SecStandardPaths)

		!insertmacro MUI_DESCRIPTION_TEXT ${SecStatisticsJob} $(DESC_SecStatisticsJob)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecMultiCameraMeasurement} $(DESC_SecMultiCameraMeasurement)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecMultiCameraAndColorStream} $(DESC_SecMultiCameraAndColorStream)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecDeviceSettingManager} $(DESC_SecDeviceSettingManager)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecWaitJob} $(DESC_SecWaitJob)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecParallelImagingJob} $(DESC_SecParallelImagingJob)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecFocusingJob} $(DESC_SecFocusingJob)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecMultiCameraStream} $(DESC_SecMultiCameraStream)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecSimpleMeasurement} $(DESC_SecSimpleMeasurement)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecCellDetection} $(DESC_SecCellDetection)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecAutofocusJob} $(DESC_SecAutofocusJob)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecUserControlMeasurement} $(DESC_SecUserControlMeasurement)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecWaitForUser} $(DESC_SecWaitForUser)

		!insertmacro MUI_DESCRIPTION_TEXT ${SecCellX} $(DESC_SecCellX) 
		!insertmacro MUI_DESCRIPTION_TEXT ${SecController} $(DESC_SecController) 
		!insertmacro MUI_DESCRIPTION_TEXT ${SecCustomJob} $(DESC_SecCustomJob) 
		!insertmacro MUI_DESCRIPTION_TEXT ${SecDropletMicrofluidics} $(DESC_SecDropletMicrofluidics) 
		;!insertmacro MUI_DESCRIPTION_TEXT ${SecFluigent} $(DESC_SecFluigent) 
		!insertmacro MUI_DESCRIPTION_TEXT ${SecNemesys} $(DESC_SecNemesys) 
		!insertmacro MUI_DESCRIPTION_TEXT ${SecLiveMeasurement} $(DESC_SecLiveMeasurement) 
		!insertmacro MUI_DESCRIPTION_TEXT ${SecRepeatJob} $(DESC_SecRepeatJob) 
		!insertmacro MUI_DESCRIPTION_TEXT ${SecSLIM} $(DESC_SecSLIM)

		!insertmacro MUI_DESCRIPTION_TEXT ${SecCustomMetadata} $(DESC_SecCustomMetadata)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecCustomSaveSettings} $(DESC_SecCustomSaveSettings)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecStandardSaveSettings} $(DESC_SecStandardSaveSettings)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecSystemSkin} $(DESC_SecSystemSkin)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecDarkSkin} $(DESC_SecDarkSkin)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecFlatLafSkin} $(DESC_SecFlatLafSkin)
		;!insertmacro MUI_DESCRIPTION_TEXT ${SecRedSkin} $(DESC_SecRedSkin)

		!insertmacro MUI_DESCRIPTION_TEXT ${SecTravelingSalesman} $(DESC_SecTravelingSalesman)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecWaitSinceLastAction} $(DESC_SecWaitSinceLastAction)
		!insertmacro MUI_DESCRIPTION_TEXT ${SecShareExecution} $(DESC_SecShareExecution)

	!insertmacro MUI_FUNCTION_DESCRIPTION_END