import { getServerEnv } from './env'

export function getAppIdentity() {
  const stage = getServerEnv().profile === 'stage'
  return {
    name: stage ? 'GenTool Data Viewer (Stage)' : 'GenTool Data Viewer',
    iconPrefix: stage ? '/icons/icon-stage' : '/icons/icon',
    favicon: stage ? '/favicon-stage.ico' : '/favicon.ico',
  }
}
