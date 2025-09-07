import { cpSync, rmSync, existsSync, mkdirSync } from 'node:fs'
import { resolve } from 'node:path'
const src = resolve('dist')
const dst = resolve('../rest-server/src/main/resources/static')
if (!existsSync(src)) { console.error('dist/ not found, build first'); process.exit(1) }
if (existsSync(dst)) rmSync(dst, { recursive: true, force: true })
mkdirSync(dst, { recursive: true })
cpSync(src, dst, { recursive: true })
console.log(`Copied UI build to ${dst}`)
