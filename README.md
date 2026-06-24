# Video Mind AI

Video Mind AI is an AI video analysis and creation workspace for short-video creators. It helps turn reference videos into structured creative materials, including scene breakdowns, rewritten scripts, storyboard notes, and prompt bundles.

## Core Workflow

```text
video upload -> async analysis -> scene breakdown -> script rewrite -> prompt bundle
```

## Features

- Upload local video files and create creator projects.
- Split large uploads into chunks and merge them on the server.
- Calculate file MD5 fingerprints for duplicate detection and result reuse.
- Track video projects, analysis status, scene segments, and rewrite drafts.
- Generate summaries, scene-level breakdowns, rewritten scripts, and prompt bundles.
- Display project results in a Vue-based creator workspace.

## Technical Highlights

- **Chunked upload and file fingerprinting**: implements upload initialization, chunk submission, merge, and server-side MD5 calculation to improve large-file reliability and avoid repeated processing.
- **Asynchronous analysis pipeline**: separates upload requests from long-running video analysis tasks, with RocketMQ integration reserved for full infrastructure mode.
- **Duplicate submission control**: uses Redisson-based distributed locking and rate-limiting design to reduce repeated AI analysis and high-frequency request pressure.
- **Structured AI output storage**: persists transcript, summary, scene segments, and rewrite drafts in relational tables so model outputs can be reused and edited.
- **Full-stack creator workspace**: combines Spring Boot APIs with a Vue 3 frontend for upload, project management, analysis status, and result viewing.

## Tech Stack

Frontend:

- Vue 3
- Vite
- Axios
- GSAP

Backend:

- Spring Boot
- MyBatis-Plus
- MySQL
- Redis
- Redisson
- RocketMQ
- MinIO

AI and media:

- FFmpeg
- ASR service integration
- SiliconFlow / DeepSeek-compatible chat completion API

## Data Model

- `media_files`: stores video project metadata, file fingerprint, transcript, summary, and analysis status.
- `scene_segments`: stores scene-level output such as actions, dialogue, shot type, emotion, and creative hooks.
- `rewrite_drafts`: stores rewritten scripts, storyboard notes, and prompt bundles.

## Local Startup

Start the demo workspace on Windows:

```bat
start-demo.cmd
```

Or start services separately:

```bat
start-demo-backend.cmd
start-demo-frontend.cmd
```

Default addresses:

- Frontend: `http://127.0.0.1:5173`
- Backend: `http://127.0.0.1:9090`

## Repository Structure

```text
client/      Vue creator workspace
server/      Spring Boot backend
docs/        project notes
scripts/     local helper scripts
```
