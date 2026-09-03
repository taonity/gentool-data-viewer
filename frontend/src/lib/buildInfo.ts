export interface BuildInfo {
  app: {
    name: string
    version: string
  }
  git: {
    commit: string
    branch: string
  }
  build: {
    time: string
  }
  runtime: {
    uptime: number
  }
}

export function getBuildInfo(): BuildInfo {
  return {
    app: {
      name: 'gentool-data-viewer-frontend',
      version: process.env.APP_VERSION || 'unknown',
    },
    git: {
      commit: process.env.GIT_COMMIT_SHA || 'unknown',
      branch: process.env.GIT_BRANCH || 'unknown',
    },
    build: {
      time: process.env.BUILD_TIME || 'unknown',
    },
    runtime: {
      uptime: process.uptime(),
    },
  }
}
