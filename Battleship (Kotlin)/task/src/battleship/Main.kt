package battleship
import kotlin.math.abs
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

data class Cell(var content: String = "~") {
    fun copy(): Cell = Cell(content)
}
enum class PlayerTurn { PLAYER1, PLAYER2 }

interface Ship {
    val name: String
    val size: Int
    var health: Int
    fun takeDamage(damage: Int) {
        health -= damage
    }
    fun isSunk(): Boolean {
        return (health == 0)
    }

    val occupiedCells: MutableSet<Pair<Char, Int>>
    fun occupyCell(row: Char, column: Int ) {
        occupiedCells.add(Pair(row, column))
    }
}


class AircraftCarrier : Ship {
    override val name: String = "Aircraft Carrier"
    override val size = 5
    override val occupiedCells = mutableSetOf<Pair<Char, Int>>()
    override var health = size
}

class Battleship : Ship {
    override val name: String = "Battleship"
    override val size = 4
    override val occupiedCells = mutableSetOf<Pair<Char, Int>>()

    override var health = size

}

class Submarine: Ship {
    override val name: String = "Submarine"
    override val size = 3
    override val occupiedCells = mutableSetOf<Pair<Char, Int>>()

    override var health = size
}

class Cruiser: Ship {
    override val name: String = "Cruiser"
    override val size = 3
    override val occupiedCells = mutableSetOf<Pair<Char, Int>>()

    override var health = size

}

class Destroyer : Ship{
    override val name: String = "Destroyer"
    override val size = 2
    override val occupiedCells = mutableSetOf<Pair<Char, Int>>()
    override var health = size
}

class Game(name: String): Thread(name) {
    var shipsSunk = 0
    val board = Board(this)
    val ships = listOf(AircraftCarrier(), Battleship(), Submarine(), Cruiser(), Destroyer())
    fun startGameShips() {
        board.printFoggedBoard()
        for (ship in ships) {
            placeShips(ship)
        }
    }
    private fun placeShips(ship: Ship) {
        println("\nEnter the coordinates of the ${ship.name} (${ship.size} cells):\n")
        while (true) {
            val placeShip = board.placeShip(ship)
            if (!placeShip) {
                continue
            }
            break
        }
    }
}

open class Board (private val game: Game) {

    private val rows = 10
    private val columns = 10
    private val table = MutableList(rows) { MutableList(columns) { Cell() } }
    private val tableMap = table.withIndex().associate { (index, row) ->
        'A' + index to row
    }
    private val foggedMap = tableMap.mapValues { (_, row) -> row.map { cell -> cell.copy() } }
    private val occupiedCells = mutableSetOf<Pair<Char, Int>>()

    fun printFoggedBoard() {
        println()
        val columnHeader = (1..columns).joinToString(" ") { it.toString() }
        println("  $columnHeader")
        for (row in foggedMap) {
            println("${row.key} ${row.value.joinToString (" ") { it.content } }")
        }
    }

    fun printBoard() {
        val columnHeader = (1..columns).joinToString(" ") { it.toString() }
        println("  $columnHeader")
        for (row in tableMap) {
            println("${row.key} ${row.value.joinToString (" ") { it.content } }")
        }
    }

    private fun overLapError(row: Char, column: Int): Boolean {
        if (tableMap[row]?.get(column)?.content != "~") {
            println("Error! Overlaps with another ship. Try again: ")
            return false
        }
        return true
    }
    private fun toCloseError(row: Char, column: Int): Boolean {
        for (i in row - 1..row + 1) {
            for (j in column - 1..column + 1) {
                if (i == row && j == column) continue //Skip current cell
                if (i !in 'A'..'J' || j !in 0 until columns) continue
                if (occupiedCells.contains(Pair(i, j))) {
                    println("\nError! You placed it too close to another one. Try again:\n")
                    return false
                }
            }
        }
        return true
    }
    private fun isVerticallyAdjacent(row: Char, column: Int): Boolean {
        for (i in row - 1..row + 1) {
            if (i != row && i in 'A'..'J' && occupiedCells.contains(Pair(i, column))) {
                println("\nError! You placed it too close to another one. Try again:\n")
                return true // Found a ship vertically adjacent

            }
        }
        return false // No vertically adjacent ships found
    }
    private fun getCoordinates(ship: Ship): List <String> {
        while (true){
            try{
                val coordinate = readln().split(" ")
                val (fromRow, fromCol) = coordinate[0][0].uppercaseChar() to coordinate[0].substring(1).toInt() - 1
                val (toRow, toCol) = coordinate[1][0].uppercaseChar() to coordinate[1].substring(1).toInt() - 1

                if (!isValidShipPlacement(ship, fromRow, toRow, fromCol, toCol)) continue

                return listOf(fromRow.toString(), fromCol.toString(), toRow.toString(), toCol.toString() )
            } catch (e: IndexOutOfBoundsException){
                println("\nWrong input, enter proper coordinates!\n")
            }
        }
    }

    private fun isValidShipPlacement(ship: Ship, rowFrom: Char, rowTo: Char, colFrom: Int, colTo: Int): Boolean {
        val isHorizontal = rowFrom == rowTo
        val isVertical = colFrom == colTo

        if (!isHorizontal && !isVertical) return false
        val length = if (isHorizontal) abs(colFrom - colTo) + 1 else abs(rowFrom - rowTo) + 1
        if (length != ship.size) return false

        for ( row in minOf(rowFrom, rowTo)..maxOf(rowFrom, rowTo)) {
            for (col in minOf(colFrom, colTo)..maxOf(colFrom, colTo)) {
                if (!overLapError(row, col) ||
                    !toCloseError(row, col) ||
                    (isVertical && isVerticallyAdjacent(row, col))
                    ) {
                    return false
                }
            }
        }
        return true
    }

    fun placeShip(ship: Ship): Boolean {
        val coordinates = getCoordinates(ship)
        val (rowFrom, columnFrom) = coordinates[0].first() to coordinates[1].toInt()
        val (rowTo, columnTo) = coordinates[2].first() to coordinates[3].toInt()

        val rowRange = minOf(rowFrom, rowTo)..maxOf(rowFrom, rowTo)
        val columnRange = minOf(columnFrom, columnTo)..maxOf(columnFrom, columnTo)
        if (!isValidShipPlacement(ship, rowFrom, rowTo, columnFrom, columnTo)) {
            return false
        }
        for (i in rowRange) {
            for (j in columnRange) {
                tableMap[i]?.get(j)?.content = "O"
                occupiedCells.add(Pair(i, j))
                ship.occupyCell(i, j)
            }
        }
        println()
        printBoard()
        return true
    }

    fun takeShot() {
        while (true) {
            val input = readln()
            val row = input[0].uppercase().first()
            val cell = input.substring(1).toInt() - 1
            if (row !in 'A'..'J' || cell !in 0 until columns) {
                println("\nError! You entered the wrong coordinates! Try again:\n")
                continue
            } else if (occupiedCells.contains(Pair(row, cell))) {
                if (tableMap[row]?.get(cell)?.content == "X") {
                    println("\nYou hit a ship!\n")
                    return
                }
                tableMap[row]?.get(cell)?.content = "X"
                foggedMap[row]?.get(cell)?.content = "X"
                for (ship in game.ships) {
                    if (ship.occupiedCells.contains(Pair(row,cell))) {
                        ship.takeDamage(1)
                        ship.occupiedCells.remove(Pair(row,cell))
                        if (ship.isSunk()) {
                            game.shipsSunk++
                            if (game.shipsSunk == 5) {
                                return
                            }
                            println("\nYou sank a ship! Specify a new target:\n")
                            return
                        }
                        println("\nYou hit a ship!\n")
                        return
                    }
                }
                return

            } else {
                foggedMap[row]?.get(cell)?.content = "M"
                tableMap[row]?.get(cell)?.content = "M"
                println("\nYou missed!\n")
                return
            }
        }
    }
}

class  GameState(val player1: Game, val player2: Game) {
    var currentTurn = PlayerTurn.PLAYER1
    var gameOver = false

    suspend fun switchTurn(channel: Channel<Unit>) {
        currentTurn = if (currentTurn == PlayerTurn.PLAYER1) PlayerTurn.PLAYER2 else PlayerTurn.PLAYER1
        channel.send(Unit)
    }
    fun getTurn(): PlayerTurn {
        return currentTurn
    }
    fun checkGameOver(): Boolean {
        return player1.shipsSunk == 5 || player2.shipsSunk == 5
    }

    fun fireShot() {
        if (currentTurn == PlayerTurn.PLAYER1) {
            player2.board.takeShot()
        } else player1.board.takeShot()
    }
}

class RunningGame {
    private fun handleShooting(threadId: String, gameState: GameState): Boolean {
        val currentPlayer = if (gameState.currentTurn == PlayerTurn.PLAYER1) gameState.player1 else gameState.player2
        val opponent = if (gameState.currentTurn == PlayerTurn.PLAYER1) gameState.player2 else gameState.player1
        opponent.board.printFoggedBoard()
        println("---------------------")
        currentPlayer.board.printBoard()

        println("\n$threadId, its your turn:\n")
        gameState.fireShot()

        if (gameState.checkGameOver()) {
            gameState.gameOver = true
            return false
        }

        println("\nPress Enter and pass the move to another player\n")
        readln()
        return true
    }

    suspend fun runTurn(threadId: String, gameState: GameState, channel: Channel<Unit>, completionSignal: CompletableDeferred<Unit>) {
        while (!gameState.gameOver) {
            if ((gameState.getTurn() == PlayerTurn.PLAYER2 && threadId == "Player 1") || (gameState.getTurn() == PlayerTurn.PLAYER1 && threadId == "Player 2")) {
                val shoot = handleShooting(threadId, gameState)
                if (!shoot) {
                    gameState.gameOver = true
                    break
                }
                gameState.switchTurn(channel)
            } else {
                channel.receive()
            }
        }
        if (gameState.gameOver) {
            completionSignal.complete(Unit)
        }
    }
}

fun main() = runBlocking {
    val p1 = Game("Player 1")
    val p2 = Game("Player 2")
    val runningGame = RunningGame()

    println("Player 1, place your ships on the game field")
    p1.startGameShips()
    println("\nPress Enter and pass the move to another player\n")
    readln()
    println("Player 2, place your ships to the game field\n")
    p2.startGameShips()
    println("\nPress Enter and pass the move to another player\n")
    readln()

    val channel = Channel<Unit>()
    val gameState = GameState(p1, p2)
    val completionSignal = CompletableDeferred<Unit>()
    val turn1 = launch { runningGame.runTurn("Player 1", gameState, channel, completionSignal) }
    val turn2 = launch { runningGame.runTurn("Player 2", gameState, channel, completionSignal) }

    completionSignal.await()
    turn1.cancelAndJoin()
    turn2.cancelAndJoin()

    println("\nYou sank the last ship. You won. Congratulations!")

}