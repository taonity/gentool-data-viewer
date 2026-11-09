const { execSync } = require('child_process')
const packageJson = require('./package.json')

function git(command) {
  try {
    return execSync(`git ${command}`, { encoding: 'utf8' }).trim()
  } catch {
    return 'unknown'
  }
}

/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  output: 'standalone',
  env: {
    APP_VERSION: packageJson.version,
    BUILD_TIME: process.env.BUILD_TIME || new Date().toISOString(),
    GIT_COMMIT_SHA: process.env.GIT_COMMIT_SHA || git('rev-parse HEAD'),
    GIT_BRANCH: process.env.GIT_BRANCH || git('rev-parse --abbrev-ref HEAD'),
    NEXT_PUBLIC_GITHUB_REPO_URL:
      process.env.NEXT_PUBLIC_GITHUB_REPO_URL || packageJson.repository,
  },
}
module.exports = nextConfig
