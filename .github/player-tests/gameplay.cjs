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
// mineflayer waits for a 5x5 chunk square with a fixed 10 s limit. A slow server start or a
// spawn on a chunk border must not fail the run once the client's own chunk has arrived.
async function chunksLoaded() {
  for (let attempt = 0; attempt < 6; attempt++) {
    try { return await bot.waitForChunksToLoad() } catch (error) {
      if (bot.world.getColumnAt(bot.entity.position)) return
    }
  }
  throw new Error('Client chunks never loaded')
}
async function consoleCommand(command) { emit({ console: command }); await delay(300) }
async function chat(command, response) {
  const start = messages.length
  bot.chat(command)
  if (response) await until(() => messages.slice(start).some(m => m.includes(response)), command)
  else await delay(300)
}
// Builds test terrain only after the server confirms it. Chunks away from the player are not
// loaded (no spawn chunks on newer versions), and an unverified fill fails silently, leaving the
// client inside natural terrain of a random world.
async function build(dimension, [x1, z1, x2, z2], fills) {
  const scoped = command => `execute in minecraft:${dimension} run ${command}`
  await consoleCommand(scoped(`forceload add ${x1} ${z1} ${x2} ${z2}`))
  for (const fill of fills) {
    const end = Date.now() + 20000
    for (;;) {
      const start = messages.length
      bot.chat('/' + scoped(fill))
      const done = () => messages.slice(start).some(m => m.includes('Successfully filled') || m.includes('No blocks were filled'))
      const answered = () => done() || messages.length > start
      for (let waited = 0; !answered() && waited < 2000; waited += 100) await delay(100)
      if (done()) break
      if (Date.now() > end) throw new Error(`Test terrain was not built: ${fill}; ${messages.slice(-5).join('; ')}`)
      await delay(500)
    }
  }
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
  await chunksLoaded()
  await consoleCommand(`op ${name}`)
  await consoleCommand('difficulty peaceful')
  await consoleCommand(`gamemode survival ${name}`)
  await build('overworld', [-2, -2, 10, 2], ['fill -2 64 -2 10 64 2 minecraft:stone', 'fill -2 65 -2 10 68 2 minecraft:air'])
  await consoleCommand(`tp ${name} 0.5 65 0.5`)
  await until(() => near(0.5) && Math.abs(bot.entity.position.y - 65) < 0.15, 'arrival on the test platform')
  if (clock) {
    patch('world/fiw-clock/config.json', { afk: { enabled: false, minutes: 1 } })
    await consoleCommand('tickwatch reload')
    await clockStatus(false)
    await chat('/tickwatch stats', `Joins: ${phase.includes('restart') ? 2 : 1}`)
  }
  if (admin) {
    // A frozen player cannot break blocks; once unfrozen the same block breaks.
    const blockIs = async (type, marker) => {
      const start = messages.length
      await consoleCommand(`execute if block 1 65 0 minecraft:${type} run tellraw ${name} {"text":"${marker}"}`)
      await until(() => messages.slice(start).some(m => m.includes(marker)), `block at 1 65 0 is ${type}`)
    }
    const dig = () => bot.dig(bot.blockAt(bot.entity.position.offset(1, 0, 0).floored()), true).catch(() => {})
    await consoleCommand('setblock 1 65 0 minecraft:dirt')
    await blockIs('dirt', 'QA_BLOCK_PLACED')
    await until(() => bot.blockAt(bot.entity.position.offset(1, 0, 0).floored())?.name === 'dirt', 'client sees the test block')
    const toggleFreeze = async reply => {
      const start = messages.length
      await consoleCommand(`fiw freeze ${name}`)
      await until(() => messages.slice(start).some(m => m.includes(reply)), `freeze toggle: ${reply}`)
    }
    await toggleFreeze('You have been frozen')
    await dig()
    await delay(500)
    await blockIs('dirt', 'QA_BLOCK_KEPT')
    await toggleFreeze('You have been unfrozen')
    await dig()
    await blockIs('air', 'QA_BLOCK_BROKEN')
    emit({ assertion: 'a frozen player cannot break a block; after unfreezing the same block breaks' })
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
    // Breaking a block during the warmup cancels it too. A torch breaks instantly, well inside
    // the 2 s warmup (the client stands still with physics off, which slows normal digging).
    await consoleCommand('setblock 7 65 0 minecraft:torch')
    const beside = () => bot.blockAt(bot.entity.position.offset(1, 0, 0).floored())
    await until(() => beside()?.name === 'torch', 'client sees the warmup test block')
    await chat('/home qa', "don't move")
    bot.dig(beside(), true).catch(() => {})
    await until(() => messages.some(m => m.includes('Teleport cancelled: you broke a block.')), 'block-break cancellation')
    await delay(2300)
    assert(near(6.5), 'warmup cancelled by a block break did not teleport')
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
    await build('the_nether', [7, -1, 10, 2], ['fill 7 119 -1 10 119 2 minecraft:stone', 'fill 7 120 -1 10 123 2 minecraft:air'])
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
    emit({ assertion: `homes, back, warmup cancellation (move and block break), warp accept/deny and dimension policy work (${phase})` })
  }
  const ended = once(bot, 'end')
  bot.quit('Gameplay verification complete')
  await ended
  clearTimeout(deadline)
  emit({ passed: phase, version, mods: { admin, clock, warp } })
}
main().catch(error => { emit({ error: error.stack }); bot.quit(); process.exit(1) })
