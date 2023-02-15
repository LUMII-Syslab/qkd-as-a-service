@echo off
:: Downloads and installs cygwin64 for windows and
:: invokes the child script.
::
:: Copyright (c) Institute of Mathematics and Computer Science, University of Latvia
:: Licence: MIT
:: Contributors:
::   Sergejs Kozlovics, 2022

if exist C:\cygwin64\bin\dos2unix.exe goto DOS2UNIX_OK

set CYGWIN_INSTALLER_PATH=cygwin_installer
set CYGWIN_SITE=https://ftp.fsn.hu/pub/cygwin/

mkdir %CYGWIN_INSTALLER_PATH%
pushd %CYGWIN_INSTALLER_PATH%

:: download cygwin installer
if not exist setup-x86_64.exe curl.exe -o setup-x86_64.exe https://www.cygwin.com/setup-x86_64.exe

:: install default cygwin packages
if not exist C:\cygwin64\bin setup-x86_64.exe -q --wait --site %CYGWIN_SITE%

:: install additional cygwin packages
setup-x86_64.exe -q --wait -P dos2unix
:: ^^^ dos2unix is needed if git added Windows CR+LF; we need to convert them back to Unix style (for Cygwin bash)

popd

:: === cygwin64 and packages installed ====
:DOS2UNIX_OK

C:\cygwin64\bin\dos2unix -q "%~dp0\ca_init.sh"
C:\cygwin64\bin\bash "%~dp0\ca_init.sh"
