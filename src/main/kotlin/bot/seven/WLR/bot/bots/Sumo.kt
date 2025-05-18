package bot.seven.WLR.bot.bots

import bot.seven.WLR.bot.BotBase
import bot.seven.WLR.bot.StateManager
import bot.seven.WLR.bot.player.*
import bot.seven.WLR.core.Config
import bot.seven.WLR.utils.*
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

    private val STRAFE_MIN_DISTANCE_OPPONENT_OLD = 3.0
    private val STRAFE_EDGE_DIFFERENCE_THRESHOLD_OLD = 1.0
    private val MIN_COMBO_TO_CLEAR_STRAFE_OLD = 2

    private var tapping = false
    private var opponentOffEdge = false
    private var tap50 = false
    private var canDistanceJump = true

    private val maxAttackDistanceConfigurable: Int
        get() = Config.maxDistanceAttack

    override fun onJoinGame() {
        if (Config.lobbyMovement) {
            LobbyMovement.sumo()
        }
        if (Config.enableCustomCamera && mc.thePlayer != null) {
            Camera.enable()
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
        if (Config.enableCustomCamera && mc.thePlayer != null) {
            Camera.enable()
        }
    }

    override fun onGameEnd() {
        Camera.disable()
        TimeUtils.setTimeout(fun () {
            Mouse.stopLeftAC()
            Combat.stopRandomStrafe()
            Mouse.stopTracking()
            Movement.startSprinting()
            Movement.startForward()
            Movement.startJumping()
        }, RandomUtils.randomIntInRange(100, 300))
    }

    override fun onAttack() {
        if (!tapping && StateManager.state == StateManager.States.PLAYING && mc.thePlayer != null) {
            tapping = true
            val dur = if (tap50) 50 else 100
            ChatUtils.info("W-Tap ($dur ms)")
            Combat.wTap(dur)
            tap50 = !tap50
            TimeUtils.setTimeout(fun () {
                tapping = false
            }, (dur.toLong() + 15).toInt())
        }
    }

    override fun onFoundOpponent() {
        if (StateManager.state == StateManager.States.PLAYING && mc.thePlayer != null && opponent() != null) {
            Mouse.startTracking()
        }
    }

    fun leftEdge(distance: Float): Boolean {
        return mc.thePlayer?.let { p -> WorldUtils.airOnLeft(p, distance) } ?: false
    }

    fun rightEdge(distance: Float): Boolean {
        return mc.thePlayer?.let { p -> WorldUtils.airOnRight(p, distance) } ?: false
    }

    fun nearEdge(distance: Float): Boolean {
        return mc.thePlayer?.let { p ->
            WorldUtils.airOnRight(p, distance) || WorldUtils.airOnLeft(p, distance) || WorldUtils.airInBack(p, distance)
        } ?: false
    }

    fun opponentNearEdge(distance: Float): Boolean {
        return opponent()?.let { opp ->
            WorldUtils.airInBack(opp, distance) || WorldUtils.airOnLeft(opp, distance) || WorldUtils.airOnRight(opp, distance)
        } ?: false
    }

    override fun onTick() {
        val player = mc.thePlayer
        val currentOpponent = opponent()

        if (StateManager.state == StateManager.States.GAME && player != null) {
            if (Config.lobbyMovement) { // Check the config setting
                if (!Movement.forward()) {
                    Movement.startForward()
                }
                if (!Movement.sprinting()) {
                    Movement.startSprinting()
                }
                if (player.onGround && !Movement.jumping()) {
                    Movement.startJumping()
                }
            } else { // If lobbyMovement is disabled, clear all movement
                Movement.clearAll()
            }
            return
        }

        if (player == null || currentOpponent == null) {
            val isMoving = Movement.forward() || Movement.backward() || Movement.left() || Movement.right()
            if (isMoving) {
                Movement.clearAll()
                Combat.stopRandomStrafe()
            }
            Mouse.stopTracking()
            Mouse.stopLeftAC()
            if (currentOpponent == null) {
                opponentOffEdge = false
            }
            return
        }

        val isCurrentOpponentActuallyOffEdge = WorldUtils.entityOffEdge(currentOpponent)
        opponentOffEdge = isCurrentOpponentActuallyOffEdge || (opponentOffEdge && EntityUtils.getDistanceNoY(player, currentOpponent) > 17)

        if (!opponentOffEdge && StateManager.state == StateManager.States.PLAYING) {
            if (!player.isSprinting) {
                Movement.startSprinting()
            }

            Mouse.startTracking()

            val distance = EntityUtils.getDistanceNoY(player, currentOpponent)

            if (distance > maxAttackDistanceConfigurable) {
                Mouse.stopLeftAC()
            } else {
                Mouse.startLeftAC()
            }

            var performingJump = false

            val jumpDistanceThreshold = RandomUtils.randomDoubleInRange(5.5, 7.0)
            if (canDistanceJump && distance >= jumpDistanceThreshold && player.onGround && !WorldUtils.airInFront(player, 3f) && !tapping) {
                Movement.clearLeftRight(); Combat.stopRandomStrafe()
                Movement.startForward()
                Movement.singleJump(RandomUtils.randomIntInRange(100, 150))
                canDistanceJump = false
                TimeUtils.setTimeout(fun() { canDistanceJump = true }, RandomUtils.randomIntInRange(500, 1000))
                performingJump = true
            }

            if (!performingJump && combo >= 3 && distance >= 3.2 && distance < (jumpDistanceThreshold - 0.5)  && player.onGround && !nearEdge(4f) && !WorldUtils.airInFront(player, 3f) && !tapping) {
                Movement.clearLeftRight(); Combat.stopRandomStrafe()
                Movement.singleJump(RandomUtils.randomIntInRange(100, 150))
                performingJump = true
            }

            if (!performingJump) {
                val movePriority = arrayListOf(0, 0)
                var clearStrafingInputs = false
                var engageRandomStrafe = false

                if (distance <= STRAFE_MIN_DISTANCE_OPPONENT_OLD) {
                    clearStrafingInputs = true
                } else if (combo >= MIN_COMBO_TO_CLEAR_STRAFE_OLD) {
                    clearStrafingInputs = true
                }

                if (!clearStrafingInputs && !tapping) {
                    val le = WorldUtils.distanceToLeftEdge(player)
                    val re = WorldUtils.distanceToRightEdge(player)
                    val diff = abs(le - re)

                    if (diff > STRAFE_EDGE_DIFFERENCE_THRESHOLD_OLD) {
                        if (le < re) {
                            movePriority[1] += 5
                        } else if (re < le) {
                            movePriority[0] += 5
                        } else {
                            engageRandomStrafe = true
                        }
                    } else {
                        engageRandomStrafe = true
                    }
                }

                if (clearStrafingInputs) {
                    Combat.stopRandomStrafe()
                    Movement.clearLeftRight()
                } else if (!tapping) {
                    if (engageRandomStrafe) {
                        Movement.clearLeftRight()
                        Combat.startRandomStrafe(900, 1400)
                    } else {
                        Combat.stopRandomStrafe()
                        if (movePriority[0] > movePriority[1]) {
                            Movement.stopRight()
                            Movement.startLeft()
                        } else if (movePriority[1] > movePriority[0]) {
                            Movement.stopLeft()
                            Movement.startRight()
                        } else {
                            Movement.clearLeftRight()
                            Combat.startRandomStrafe(900, 1400)
                        }
                    }
                } else if (tapping && !clearStrafingInputs) {
                    Combat.stopRandomStrafe()
                    Movement.clearLeftRight()
                }

                if (!tapping) {
                    if (distance < 1.2) {
                        Movement.stopForward()
                    } else {
                        Movement.startForward()
                    }
                }

                if (WorldUtils.airInFront(player, 1.75f) && player.onGround) {
                    Movement.startSneaking()
                    Movement.stopForward()
                    Movement.clearLeftRight()
                    Combat.stopRandomStrafe()
                } else {
                    Movement.stopSneaking()
                }

                if (WorldUtils.airInBack(player, 2.0f) && player.onGround) {
                    Movement.clearLeftRight()
                    Combat.stopRandomStrafe()
                    if (!tapping) {
                        Movement.startForward()
                    }
                }

                if (Movement.left() && WorldUtils.airOnLeft(player, 1.5f) && player.onGround) {
                    Movement.stopLeft()
                }
                if (Movement.right() && WorldUtils.airOnRight(player, 1.5f) && player.onGround) {
                    Movement.stopRight()
                }
            }

        } else {
            Mouse.stopLeftAC()
            Combat.stopRandomStrafe()
            Mouse.stopTracking()
            if (StateManager.state == StateManager.States.PLAYING && opponentOffEdge) {
                Movement.clearAll()
            }
        }
    }
}