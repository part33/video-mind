@echo off
setlocal

set "ROOT=%~dp0"

start "VideoMind AI Backend" cmd /k call "%ROOT%start-demo-backend.cmd"
start "VideoMind AI Frontend" cmd /k call "%ROOT%start-demo-frontend.cmd"

echo Backend: http://localhost:9090
echo Frontend: http://127.0.0.1:5173
echo Open the frontend, register a demo account, then upload a reference video to test the workflow.
