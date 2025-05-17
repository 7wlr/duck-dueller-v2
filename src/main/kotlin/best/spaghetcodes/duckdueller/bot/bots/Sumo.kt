package best.spaghetcodes.duckdueller.bot.bots

import best.spaghetcodes.duckdueller.DuckDueller
import best.spaghetcodes.duckdueller.bot.BotBase
import best.spaghetcodes.duckdueller.bot.StateManager
import best.spaghetcodes.duckdueller.bot.player.Combat
import best.spaghetcodes.duckdueller.bot.player.LobbyMovement
import best.spaghetcodes.duckdueller.bot.player.Mouse
import best.spaghetcodes.duckdueller.bot.player.Camera
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

    private val STRAFE_MIN_DISTANCE_OPPONENT_OLD = 3.0
    private val STRAFE_EDGE_DIFFERENCE_THRESHOLD_OLD = 1.0
    private val MIN_COMBO_TO_CLEAR_STRAFE_OLD = 2

    private var tapping = false
    private var opponentOffEdge = false
    private var tap50 = false
    private var canDistanceJump = true

    private val minAttackDistance = 3.0
    private val maxAttackDistanceConfigurable: Int
        get() = DuckDueller.config?.maxDistanceAttack ?: 5


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
        if (DuckDueller.config?.enableCustomCamera == true && mc.thePlayer != null) {
            Camera.enable()
        }
    }

    override fun onGameEnd() {
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
        if (StateManager.state == StateManager.States.PLAYING && mc.thePlayer != null && opponent() != null) {
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
        return (WorldUtils.airOnRight(mc.thePlayer, distance) || WorldUtils.airOnLeft(mc.thePlayer, distance) || WorldUtils.airInBack(mc.thePlayer, distance))
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
            Mouse.stopTracking()
            Mouse.stopLeftAC()
            return
        }

        opponentOffEdge = WorldUtils.entityOffEdge(opponent()!!) || (opponentOffEdge && EntityUtils.getDistanceNoY(mc.thePlayer, opponent()!!) > 17)

        if (!opponentOffEdge && StateManager.state == StateManager.States.PLAYING) {
            if (!mc.thePlayer.isSprinting) {
                Movement.startSprinting()
            }

            Mouse.startTracking()

            val distance = EntityUtils.getDistanceNoY(mc.thePlayer, opponent()!!)

            if (distance > maxAttackDistanceConfigurable) {
                Mouse.stopLeftAC()
            } else {
                Mouse.startLeftAC()
            }

            var performingJump = false

            val jumpDistanceThreshold = RandomUtils.randomDoubleInRange(5.5, 7.0)
            if (canDistanceJump && distance >= jumpDistanceThreshold && mc.thePlayer.onGround && !WorldUtils.airInFront(mc.thePlayer, 3f) && !tapping) {
                Movement.clearLeftRight(); Combat.stopRandomStrafe()
                Movement.startForward()
                Movement.singleJump(RandomUtils.randomIntInRange(100, 150))
                canDistanceJump = false
                TimeUtils.setTimeout(fun() { canDistanceJump = true }, RandomUtils.randomIntInRange(500, 1000))
                performingJump = true
            }

            if (!performingJump && combo >= 3 && distance >= 3.2 && distance < (jumpDistanceThreshold - 0.5)  && mc.thePlayer.onGround && !nearEdge(4f) && !WorldUtils.airInFront(mc.thePlayer, 3f) && !tapping) {
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
                    val le = WorldUtils.distanceToLeftEdge(mc.thePlayer)
                    val re = WorldUtils.distanceToRightEdge(mc.thePlayer)
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
                            if (RandomUtils.randomBool()) {
                                Movement.startLeft()
                            } else {
                                Movement.startRight()
                            }
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

                if (WorldUtils.airInFront(mc.thePlayer, 1.75f) && mc.thePlayer.onGround) {
                    Movement.startSneaking()
                    Movement.stopForward()
                    Movement.clearLeftRight()
                    Combat.stopRandomStrafe()
                } else {
                    Movement.stopSneaking()
                }

                if (WorldUtils.airInBack(mc.thePlayer, 2.0f) && mc.thePlayer.onGround) {
                    Movement.clearLeftRight()
                    Combat.stopRandomStrafe()
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