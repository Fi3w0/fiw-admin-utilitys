// Exercises installed Fiw mods through the vanilla wire protocol in a disposable server.
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { once } = require('node:events')
const mineflayer = require('mineflayer')
const [port, version, phase, directory] = process.argv.slice(2)
const name = 'FiwIntegration'
const emit = value => process.stdout.write(JSON.stringify(value) + '\n')
const delay = ms => new Promise(resolve => setTimeout(resolve, ms))
const deadline = setTimeout(() => { emit({ error: 'Gameplay verification timed out' }); process.exit(1) }, 150000)
const jars = fs.readdirSync(path.join(directory, 'mods'))
const admin = jars.some(p => p.startsWith('fiw-admin-tools-'))
const clock = jars.some(p => p.startsWith('fiw-clock-'))
const warp = jars.some(p => p.startsWith('fiw-warp-'))
const bot = mineflayer.createBot({ host: '127.0.0.1', port: Number(port), username: name, auth: 'offline', version })
const messages = []
bot.on('messagestr', message => messages.push(message.replace(/§./g, '')))
bot.on('error', error => { emit({ error: error.stack }); process.exit(1) })
bot.on('kicked', reason => { emit({ error: 'Client kicked', reason }); process.exit(1) })
async function until(condition, description, timeout = 12000) {
  const end = Date.now() + timeout
  while (!condition()) {
    if (Date.now() > end) throw new Error(`Timed out: ${description}; ${messages.slice(-15).join('; ')}`)
    await delay(100)
  }
}
async function consoleCommand(command) { emit({ console: command }); await delay(300) }
async function chat(command, response) {
  const start = messages.length
  bot.chat(command)
  if (response) await until(() => messages.slice(start).some(m => m.includes(response)), command)
  else await delay(300)
}
function patch(relative, changes) {
  const file = path.join(directory, relative)
  const data = fs.existsSync(file) ? JSON.parse(fs.readFileSync(file)) : {}
  fs.mkdirSync(path.dirname(file), { recursive: true })
  fs.writeFileSync(file, JSON.stringify({ ...data, ...changes }))
}
async function clockStatus(afk) {
  await chat('/tickwatch stats', `${name} (${afk ? 'AFK' : 'online'})`)
}
const near = x => Math.abs(bot.entity.position.x - x) < 0.15
async function main() {
  await once(bot, 'spawn')
  bot.physicsEnabled = false
  await bot.waitForChunksToLoad()
  await consoleCommand(`op ${name}`)
  await consoleCommand('difficulty peaceful')
  await consoleCommand(`gamemode survival ${name}`)
  await consoleCommand('fill -2 64 -2 10 64 2 minecraft:stone')
  await consoleCommand('fill -2 65 -2 10 68 2 minecraft:air')
  await consoleCommand(`tp ${name} 0.5 65 0.5`)
  if (clock) {
    patch('world/fiw-clock/config.json', { afk: { enabled: false, minutes: 1 } })
    await consoleCommand('tickwatch reload')
    await clockStatus(false)
    await chat('/tickwatch stats', `Joins: ${phase.includes('restart') ? 2 : 1}`)
  }
  if (admin) {
    patch('config/fiw-admin/afk.json', { enabled: true, idleThresholdSeconds: 600, kickAfterSeconds: 0 })
    await consoleCommand('fiw reload')
    await chat('/fiw afk', 'You are now marked AFK.')
    await delay(1200)
    if (clock) await clockStatus(true)
    await chat('/fiw afk list', 'AFK players (1)')
    await bot.look(bot.entity.yaw + 0.7, bot.entity.pitch + 0.1, true)
    await delay(1200)
    if (clock) await clockStatus(false)
    await chat('/fiw afk list', 'No AFK players.')
    patch('config/fiw-admin/afk.json', { idleThresholdSeconds: 3 })
    await consoleCommand('fiw reload')
    await delay(4500)
    if (clock) await clockStatus(true)
    await chat('/fiw afk list', 'AFK players (1)')
    bot.chat('Returning from the automated AFK check')
    await delay(1000)
    if (clock) await clockStatus(false)
    await chat('/fiw afk', 'You are now marked AFK.')
    await delay(1200)
    if (clock) await clockStatus(true)
    patch('config/fiw-admin/afk.json', { enabled: false })
    await consoleCommand('fiw reload')
    await delay(1200)
    await chat('/fiw afk list', 'No AFK players.')
    if (clock) await clockStatus(false)
    emit({ assertion: clock ? 'actual optional AFK bridge agrees on manual, camera, timeout, chat and disabled states' : 'AFK manual, camera, timeout, chat and disabled states work' })
  }
  if (warp) {
    patch('config/fiwwarp/config.json', { warmup_seconds: 2, allow_bypass: false, effects_enabled: false,
      home_cooldown_seconds: 0, warp_cooldown_seconds: 0, back_cooldown_seconds: 0, cross_dimension: true })
    await consoleCommand('fiwwarp reload')
    if (!phase.includes('restart')) {
      await chat('/sethome qa', 'set.')
      await chat('/setwarp qa', 'created.')
    }
    await consoleCommand(`tp ${name} 5.5 65 0.5`)
    await chat('/home qa', "don't move")
    await consoleCommand(`tp ${name} 6.5 65 0.5`)
    await until(() => messages.some(m => m.includes('Teleport cancelled: you moved.')), 'movement cancellation')
    await delay(2300)
    assert(near(6.5), 'cancelled warmup did not teleport')
    await chat('/home qa', "don't move")
    await until(() => near(0.5), 'home teleport after warmup')
    await chat('/back', "don't move")
    await until(() => near(6.5), 'back returns to origin')
    await chat('/warp qa', 'Confirm within')
    await chat('/warp deny', 'cancelled.')
    assert(near(6.5), 'denied warp leaves player in place')
    await chat('/warp qa', 'Confirm within')
    await chat('/warp accept', "don't move")
    await until(() => near(0.5), 'accepted warp reaches saved destination')
    patch('config/fiwwarp/config.json', { cross_dimension: false })
    await consoleCommand('fiwwarp reload')
    await consoleCommand(`gamemode creative ${name}`)
    await consoleCommand('execute in minecraft:the_nether run fill 7 119 -1 10 119 2 minecraft:stone')
    await consoleCommand('execute in minecraft:the_nether run fill 7 120 -1 10 123 2 minecraft:air')
    await consoleCommand(`execute in minecraft:the_nether run tp ${name} 8.5 120 0.5`)
    await consoleCommand(`execute as ${name} at @s if dimension minecraft:the_nether run tellraw ${name} {"text":"QA_NETHER"}`)
    await until(() => messages.some(m => m.includes('QA_NETHER')), 'server confirmed nether transition')
    await chat('/home qa', "don't move")
    await until(() => messages.some(m => m.includes('Cross-dimension teleporting is disabled.')), 'dimension policy')
    assert(near(8.5), 'disabled cross-dimension teleport left player in place')
    patch('config/fiwwarp/config.json', { cross_dimension: true })
    await consoleCommand('fiwwarp reload')
    await chat('/home qa', "don't move")
    await until(() => near(0.5), 'enabled cross-dimension teleport returns home')
    emit({ assertion: `homes, back, warmup cancellation, warp accept/deny and dimension policy work (${phase})` })
  }
  const ended = once(bot, 'end')
  bot.quit('Gameplay verification complete')
  await ended
  clearTimeout(deadline)
  emit({ passed: phase, version, mods: { admin, clock, warp } })
}
main().catch(error => { emit({ error: error.stack }); bot.quit(); process.exit(1) })
