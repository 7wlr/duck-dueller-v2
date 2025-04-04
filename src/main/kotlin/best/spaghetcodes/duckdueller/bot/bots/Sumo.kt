package best.spaghetcodes.duckdueller.bot.bots

import best.spaghetcodes.duckdueller.DuckDueller
import best.spaghetcodes.duckdueller.bot.BotBase
import best.spaghetcodes.duckdueller.bot.StateManager
import best.spaghetcodes.duckdueller.bot.player.Combat
import best.spaghetcodes.duckdueller.bot.player.LobbyMovement
import best.spaghetcodes.duckdueller.bot.player.Mouse
import best.spaghetcodes.duckdueller.bot.player.Movement
import best.spaghetcodes.duckdueller.utils.*
import kotlin.math.abs

class Sumo : BotBase("/play duels_sumo_duel") {

    override fun getName(): String {
        return "Sumo"
    }

    init {
        setStatKeys(
            mapOf(
                "wins" to "player.stats.Duels.sumo_duel_wins",
                "losses" to "player.stats.Duels.sumo_duel_losses",
                "ws" to "player.stats.Duels.current_sumo_winstreak",
            )
        )
    }

    private var tapping = false
    private var opponentOffEdge = false
    private var tap50 = false
    private var canDistanceJump = true

    private val minAttackDistance = 3.0
    private val maxAttackDistance = 4.0

    override fun onJoinGame() {
        if (DuckDueller.config?.lobbyMovement == true) {
            LobbyMovement.sumo()
        }
    }

    override fun beforeStart() {
        LobbyMovement.stop()
        canDistanceJump = true
    }

    override fun beforeLeave() {
        LobbyMovement.stop()
    }

    override fun onGameStart() {
        LobbyMovement.stop()
        Movement.startSprinting()
        Movement.startForward()
        Movement.clearLeftRight()
        Combat.stopRandomStrafe()
        canDistanceJump = true
        tapping = false
        opponentOffEdge = false
        tap50 = false
    }

    override fun onGameEnd() {
        TimeUtils.setTimeout(fun () {
            Mouse.stopLeftAC()
            Combat.stopRandomStrafe()
            Mouse.stopTracking()
        }, RandomUtils.randomIntInRange(100, 300))
    }

    override fun onAttack() {
        if (!tapping && StateManager.state == StateManager.States.PLAYING) {
            tapping = true
            val dur = if (tap50) 50 else 100
            Combat.wTap(dur)
            tap50 = !tap50
            TimeUtils.setTimeout(fun () {
                tapping = false
            }, (dur.toLong() + 15).toInt())
        }
    }

    override fun onFoundOpponent() {
        if (StateManager.state == StateManager.States.PLAYING) {
            Mouse.startTracking()
        }
    }

    fun leftEdge(distance: Float): Boolean {
        if (mc.thePlayer == null) return false
        return WorldUtils.airOnLeft(mc.thePlayer, distance)
    }

    fun rightEdge(distance: Float): Boolean {
        if (mc.thePlayer == null) return false
        return WorldUtils.airOnRight(mc.thePlayer, distance)
    }

    fun nearEdge(distance: Float): Boolean {
        if (mc.thePlayer == null) return false
        return (rightEdge(distance) || leftEdge(distance) || WorldUtils.airInBack(mc.thePlayer, distance))
    }

    fun opponentNearEdge(distance: Float): Boolean {
        if (opponent() == null) return false
        return (WorldUtils.airInBack(opponent()!!, distance) || WorldUtils.airOnLeft(opponent()!!, distance) || WorldUtils.airOnRight(opponent()!!, distance))
    }

    override fun onTick() {
        if (mc.thePlayer == null || opponent() == null) {
            val isMoving = Movement.forward() || Movement.backward() || Movement.left() || Movement.right()
            if (isMoving) {
                Movement.clearAll()
                Combat.stopRandomStrafe()
            }
            return
        }

        opponentOffEdge = WorldUtils.entityOffEdge(opponent()!!) || (opponentOffEdge && EntityUtils.getDistanceNoY(mc.thePlayer, opponent()!!) > 9) // fixed the bot crashing basically :sob:

        if (!opponentOffEdge && StateManager.state == StateManager.States.PLAYING) {
            if (!mc.thePlayer.isSprinting) {
                Movement.startSprinting()
            }

            Mouse.startTracking()

            val distance = EntityUtils.getDistanceNoY(mc.thePlayer, opponent()!!)

            val currentAttackThreshold = RandomUtils.randomDoubleInRange(minAttackDistance, maxAttackDistance)

            if (distance > currentAttackThreshold) {
                Mouse.stopLeftAC()
            } else {
                Mouse.startLeftAC()
            }

            Combat.stopRandomStrafe()

            val jumpDistanceThreshold = RandomUtils.randomDoubleInRange(5.5, 7.0)
            if (canDistanceJump && distance >= jumpDistanceThreshold && mc.thePlayer.onGround && !WorldUtils.airInFront(mc.thePlayer, 3f) && !tapping) {
                Movement.clearLeftRight()
                Movement.startForward()
                Movement.singleJump(RandomUtils.randomIntInRange(100, 150))
                canDistanceJump = false
                TimeUtils.setTimeout(fun() { canDistanceJump = true }, RandomUtils.randomIntInRange(500, 1000))
            } else {
                if (combo >= 3 && distance >= 3.2 && distance < jumpDistanceThreshold - 0.5 && mc.thePlayer.onGround && !nearEdge(4f) && !WorldUtils.airInFront(mc.thePlayer, 3f) && !tapping) {
                    Movement.clearLeftRight()
                    Movement.singleJump(RandomUtils.randomIntInRange(100, 150))
                }

                if (!tapping) {
                    if (distance < 1.2) {
                        Movement.stopForward()
                    } else {
                        if (!WorldUtils.airInFront(mc.thePlayer, 1.75f) || !mc.thePlayer.onGround) {
                            Movement.startForward()
                        }
                    }
                }

                if (WorldUtils.airInFront(mc.thePlayer, 1.75f) && mc.thePlayer.onGround) {
                    Movement.startSneaking()
                    Movement.stopForward()
                    Movement.clearLeftRight()
                } else {
                    Movement.stopSneaking()
                }

                if (WorldUtils.airInBack(mc.thePlayer, 2.0f) && mc.thePlayer.onGround) {
                    Movement.clearLeftRight()
                    if (!tapping) {
                        Movement.startForward()
                    }
                }

                if (Movement.left() && WorldUtils.airOnLeft(mc.thePlayer, 1.5f) && mc.thePlayer.onGround) {
                    Movement.stopLeft()
                }
                if (Movement.right() && WorldUtils.airOnRight(mc.thePlayer, 1.5f) && mc.thePlayer.onGround) {
                    Movement.stopRight()
                }

                if (!tapping &&
                    !(WorldUtils.airInBack(mc.thePlayer, 2.0f) && mc.thePlayer.onGround) &&
                    !(Movement.left() && WorldUtils.airOnLeft(mc.thePlayer, 1.5f) && mc.thePlayer.onGround) &&
                    !(Movement.right() && WorldUtils.airOnRight(mc.thePlayer, 1.5f) && mc.thePlayer.onGround)
                ) {
                }
            }
        } else {
            Mouse.stopLeftAC()
            Combat.stopRandomStrafe()
            Mouse.stopTracking()
        }
    }
}
