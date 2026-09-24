package org.myapp

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

enum class PuzzleDifficulty(
    val pieces: Int,
    val label: String
) {
    EASY(9, "Easy"),
    MEDIUM(16, "Medium"),
    HARD(25, "Hard"),
    IMPOSSIBLE(64, "Impossible")
}

private const val MARGIN_FRACTION = 0.28f
private const val BUMP_FRACTION = 0.20f

data class PuzzlePieceSpec(
    val row: Int,
    val col: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val left: Int,
    val bitmap: Bitmap
)

class PuzzleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                PuzzleScreen()
            }
        }
    }
}

@Composable
fun PuzzleScreen() {
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    var sourceBitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }

    var difficulty by remember {
        mutableStateOf(PuzzleDifficulty.EASY)
    }

    var columns by remember {
        mutableIntStateOf(0)
    }

    var rows by remember {
        mutableIntStateOf(0)
    }

    var cellSizeDp by remember {
        mutableStateOf(0.dp)
    }

    var pieceSpecs by remember {
        mutableStateOf<List<PuzzlePieceSpec>>(emptyList())
    }

    var pieceOffsets by remember {
        mutableStateOf<List<Offset>>(emptyList())
    }

    var placed by remember {
        mutableStateOf<List<Boolean>>(emptyList())
    }

    var isSolved by remember {
        mutableStateOf(false)
    }

    val screenWidthDp =
        configuration.screenWidthDp.dp

    val screenHeightDp =
        configuration.screenHeightDp.dp

    val topUiSpace = 270.dp
    val bottomPadding = 32.dp
    val gap = 16.dp

    val availableWidthDp =
        (screenWidthDp - 32.dp)
            .coerceAtLeast(120.dp)

    val availableHeightDp =
        (
            screenHeightDp -
                topUiSpace -
                bottomPadding
        ).coerceAtLeast(120.dp)

    fun calculateGrid(
        bitmap: Bitmap,
        selectedDifficulty: PuzzleDifficulty
    ): Pair<Int, Int> {

        val aspect =
            bitmap.width.toFloat() /
                bitmap.height.toFloat()

        val target =
            selectedDifficulty.pieces

        var bestColumns = 1
        var bestRows = target
        var bestScore = Float.MAX_VALUE

        for (candidateColumns in 1..target) {

            val idealRows =
                target.toFloat() /
                    candidateColumns

            val candidates =
                listOf(
                    idealRows.roundToInt(),
                    idealRows.toInt(),
                    idealRows.toInt() + 1
                )

            for (candidateRowsRaw in candidates) {

                val candidateRows =
                    candidateRowsRaw
                        .coerceIn(1, target)

                val count =
                    candidateColumns *
                        candidateRows

                if (count < 1) {
                    continue
                }

                val gridAspect =
                    candidateColumns.toFloat() /
                        candidateRows

                val aspectError =
                    abs(
                        kotlin.math.ln(
                            gridAspect /
                                aspect
                        )
                    )

                val countError =
                    abs(
                        count -
                            target
                    ) * 0.04f

                val score =
                    aspectError +
                        countError

                if (score < bestScore) {
                    bestScore = score
                    bestColumns =
                        candidateColumns
                    bestRows =
                        candidateRows
                }
            }
        }

        return bestColumns to bestRows
    }

    fun startPuzzle(
        selectedDifficulty: PuzzleDifficulty = difficulty
    ) {

        val bitmap =
            sourceBitmap ?: return

        val grid =
            calculateGrid(
                bitmap,
                selectedDifficulty
            )

        val gridColumns =
            grid.first

        val gridRows =
            grid.second

        val maxCellWidth =
            availableWidthDp /
                gridColumns

        val maxCellHeight =
            availableHeightDp /
                gridRows

        val selectedCellSize =
            minOf(
                maxCellWidth,
                maxCellHeight,
                72.dp
            )

        columns =
            gridColumns

        rows =
            gridRows

        cellSizeDp =
            selectedCellSize

        val boardWidthDp =
            selectedCellSize *
                gridColumns

        val boardHeightDp =
            selectedCellSize *
                gridRows

        val marginDp =
            selectedCellSize *
                MARGIN_FRACTION

        val pieceSizeDp =
            selectedCellSize +
                marginDp * 2

        val trayHeightDp =
            pieceSizeDp * 2

        val squareCellSource =
            minOf(
                bitmap.width.toFloat() /
                    gridColumns,
                bitmap.height.toFloat() /
                    gridRows
            )

        val cropWidth =
            (
                squareCellSource *
                    gridColumns
            ).toInt()

        val cropHeight =
            (
                squareCellSource *
                    gridRows
            ).toInt()

        val cropLeft =
            (
                bitmap.width -
                    cropWidth
            ) / 2

        val cropTop =
            (
                bitmap.height -
                    cropHeight
            ) / 2

        val croppedBitmap =
            Bitmap.createBitmap(
                bitmap,
                cropLeft.coerceAtLeast(0),
                cropTop.coerceAtLeast(0),
                cropWidth.coerceAtMost(
                    bitmap.width -
                        cropLeft.coerceAtLeast(0)
                ),
                cropHeight.coerceAtMost(
                    bitmap.height -
                        cropTop.coerceAtLeast(0)
                )
            )

        val cellSourceSize =
            minOf(
                croppedBitmap.width /
                    gridColumns,
                croppedBitmap.height /
                    gridRows
            )

        val marginSource =
            (
                cellSourceSize *
                    MARGIN_FRACTION
                ).toInt()

        val hEdges =
            Array(gridRows) {
                IntArray(gridColumns)
            }

        val vEdges =
            Array(gridRows) {
                IntArray(gridColumns)
            }

        for (row in 1 until gridRows) {
            for (col in 0 until gridColumns) {
                hEdges[row][col] =
                    if (Random.nextBoolean()) {
                        1
                    } else {
                        -1
                    }
            }
        }

        for (row in 0 until gridRows) {
            for (col in 1 until gridColumns) {
                vEdges[row][col] =
                    if (Random.nextBoolean()) {
                        1
                    } else {
                        -1
                    }
            }
        }

        val specs =
            mutableListOf<PuzzlePieceSpec>()

        for (row in 0 until gridRows) {
            for (col in 0 until gridColumns) {

                val top =
                    if (row == 0) {
                        0
                    } else {
                        hEdges[row][col]
                    }

                val bottom =
                    if (row == gridRows - 1) {
                        0
                    } else {
                        -hEdges[row + 1][col]
                    }

                val left =
                    if (col == 0) {
                        0
                    } else {
                        vEdges[row][col]
                    }

                val right =
                    if (col == gridColumns - 1) {
                        0
                    } else {
                        -vEdges[row][col + 1]
                    }

                val pieceBitmap =
                    buildPieceBitmap(
                        source = croppedBitmap,
                        row = row,
                        col = col,
                        columns = gridColumns,
                        rows = gridRows,
                        marginPx = marginSource
                    )

                specs.add(
                    PuzzlePieceSpec(
                        row = row,
                        col = col,
                        top = top,
                        right = right,
                        bottom = bottom,
                        left = left,
                        bitmap = pieceBitmap
                    )
                )
            }
        }

        val boardWidthPx =
            with(density) {
                boardWidthDp.toPx()
            }

        val boardHeightPx =
            with(density) {
                boardHeightDp.toPx()
            }

        val pieceSizePx =
            with(density) {
                pieceSizeDp.toPx()
            }

        val trayTopPx =
            boardHeightPx +
                with(density) {
                    gap.toPx()
                }

        val trayHeightPx =
            with(density) {
                trayHeightDp.toPx()
            }

        val maxX =
            (
                boardWidthPx -
                    pieceSizePx
            ).coerceAtLeast(0f)

        val maxY =
            (
                trayTopPx +
                    trayHeightPx -
                    pieceSizePx
            ).coerceAtLeast(
                trayTopPx
            )

        val newOffsets =
            specs.map {
                Offset(
                    x =
                        if (maxX > 0f) {
                            Random.nextFloat() *
                                maxX
                        } else {
                            0f
                        },
                    y =
                        if (maxY > trayTopPx) {
                            trayTopPx +
                                Random.nextFloat() *
                                (
                                    maxY -
                                        trayTopPx
                                )
                        } else {
                            trayTopPx
                        }
                )
            }

        pieceSpecs =
            specs

        pieceOffsets =
            newOffsets

        placed =
            List(specs.size) {
                false
            }

        isSolved =
            false
    }

    val pickImageLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.GetContent()
        ) { uri: Uri? ->

            if (uri != null) {

                val input =
                    context.contentResolver
                        .openInputStream(uri)

                val bitmap =
                    android.graphics.BitmapFactory
                        .decodeStream(input)

                input?.close()

                if (bitmap != null) {

                    sourceBitmap =
                        bitmap

                    pieceSpecs =
                        emptyList()

                    pieceOffsets =
                        emptyList()

                    placed =
                        emptyList()

                    isSolved =
                        false

                    columns =
                        0

                    rows =
                        0

                    cellSizeDp =
                        0.dp
                }
            }
        }

    val boardWidthDp =
        cellSizeDp *
            columns

    val boardHeightDp =
        cellSizeDp *
            rows

    val marginDp =
        cellSizeDp *
            MARGIN_FRACTION

    val pieceSizeDp =
        cellSizeDp +
            marginDp * 2

    val trayHeightDp =
        pieceSizeDp * 2

    val containerHeightDp =
        boardHeightDp +
            16.dp +
            trayHeightDp

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            text =
                "Jigsaw Puzzle",
            style =
                MaterialTheme.typography
                    .headlineSmall,
            color =
                Color.White
        )

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        Button(
            onClick = {
                pickImageLauncher.launch(
                    "image/*"
                )
            }
        ) {
            Text(
                "Choose Image"
            )
        }

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        Text(
            text =
                "Difficulty",
            style =
                MaterialTheme.typography
                    .titleMedium,
            color =
                Color.White
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {

            PuzzleDifficulty.entries
                .forEach { diff ->

                    FilterChip(
                        selected =
                            difficulty == diff,
                        onClick = {

                            difficulty =
                                diff

                            if (
                                sourceBitmap !=
                                    null
                            ) {
                                startPuzzle(
                                    diff
                                )
                            }
                        },
                        label = {
                            Text(
                                diff.label
                            )
                        }
                    )
                }
        }

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        Button(
            enabled =
                sourceBitmap != null,
            onClick = {
                startPuzzle(
                    difficulty
                )
            }
        ) {
            Text(
                "Start / Shuffle"
            )
        }

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        if (
            sourceBitmap != null &&
            pieceSpecs.isNotEmpty()
        ) {

            Box(
                modifier =
                    Modifier
                        .width(
                            boardWidthDp
                        )
                        .height(
                            containerHeightDp
                        )
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(
                                boardWidthDp,
                                boardHeightDp
                            )
                            .align(
                                Alignment.TopStart
                            )
                ) {

                    Canvas(
                        modifier =
                            Modifier.fillMaxSize()
                    ) {

                        val cell =
                            size.width /
                                columns

                        val stroke =
                            1.dp.toPx()

                        for (
                            i in
                            0..columns
                        ) {

                            val x =
                                i * cell

                            drawLine(
                                color =
                                    Color.White.copy(
                                        alpha = 0.18f
                                    ),
                                start =
                                    Offset(
                                        x,
                                        0f
                                    ),
                                end =
                                    Offset(
                                        x,
                                        size.height
                                    ),
                                strokeWidth =
                                    stroke
                            )
                        }

                        for (
                            i in
                            0..rows
                        ) {

                            val y =
                                i * cell

                            drawLine(
                                color =
                                    Color.White.copy(
                                        alpha = 0.18f
                                    ),
                                start =
                                    Offset(
                                        0f,
                                        y
                                    ),
                                end =
                                    Offset(
                                        size.width,
                                        y
                                    ),
                                strokeWidth =
                                    stroke
                            )
                        }
                    }
                }

                pieceSpecs.forEachIndexed {
                    index,
                    spec ->

                    val offset =
                        pieceOffsets
                            .getOrNull(index)
                            ?: Offset.Zero

                    val isPlaced =
                        placed
                            .getOrNull(index)
                            ?: false

                    val boardWidthPx =
                        with(density) {
                            boardWidthDp.toPx()
                        }

                    val boardHeightPx =
                        with(density) {
                            boardHeightDp.toPx()
                        }

                    val pieceSizePx =
                        with(density) {
                            pieceSizeDp.toPx()
                        }

                    val marginPx =
                        with(density) {
                            marginDp.toPx()
                        }

                    val cellPx =
                        with(density) {
                            cellSizeDp.toPx()
                        }

                    val correctSlotX =
                        spec.col *
                            cellPx -
                            marginPx

                    val correctSlotY =
                        spec.row *
                            cellPx -
                            marginPx

                    val snapThresholdPx =
                        pieceSizePx *
                            0.35f

                    var currentOffset =
                        offset

                    Image(
                        bitmap =
                            spec.bitmap
                                .asImageBitmap(),
                        contentDescription =
                            null,
                        modifier =
                            Modifier
                                .size(
                                    pieceSizeDp
                                )
                                .graphicsLayer {

                                    translationX =
                                        offset.x

                                    translationY =
                                        offset.y

                                    alpha =
                                        if (
                                            isPlaced
                                        ) {
                                            0.75f
                                        } else {
                                            1f
                                        }
                                }
                                .clip(
                                    GenericShape {
                                        size, _ ->

                                        addPath(
                                            buildPiecePath(
                                                size,
                                                spec.top,
                                                spec.right,
                                                spec.bottom,
                                                spec.left
                                            )
                                        )
                                    }
                                )
                                .pointerInput(
                                    index,
                                    isPlaced
                                ) {

                                    if (isPlaced) {
                                        return@pointerInput
                                    }

                                    detectDragGestures(

                                        onDragStart = {
                                            currentOffset =
                                                pieceOffsets[
                                                    index
                                                ]
                                        },

                                        onDrag = {
                                            change,
                                            dragAmount ->

                                            change.consume()

                                            val minX =
                                                -marginPx

                                            val maxX =
                                                (
                                                    boardWidthPx -
                                                        pieceSizePx +
                                                        marginPx
                                                ).coerceAtLeast(
                                                    minX
                                                )

                                            val minY =
                                                -marginPx

                                            val containerHeightPx =
                                                with(
                                                    density
                                                ) {
                                                    containerHeightDp
                                                        .toPx()
                                                }

                                            val maxY =
                                                (
                                                    containerHeightPx -
                                                        pieceSizePx +
                                                        marginPx
                                                ).coerceAtLeast(
                                                    minY
                                                )

                                            val newX =
                                                (
                                                    currentOffset.x +
                                                        dragAmount.x
                                                ).coerceIn(
                                                    minX,
                                                    maxX
                                                )

                                            val newY =
                                                (
                                                    currentOffset.y +
                                                        dragAmount.y
                                                ).coerceIn(
                                                    minY,
                                                    maxY
                                                )

                                            currentOffset =
                                                Offset(
                                                    newX,
                                                    newY
                                                )

                                            pieceOffsets =
                                                pieceOffsets
                                                    .toMutableList()
                                                    .also {
                                                        it[index] =
                                                            currentOffset
                                                    }
                                        },

                                        onDragEnd = {

                                            val dx =
                                                currentOffset.x -
                                                    correctSlotX

                                            val dy =
                                                currentOffset.y -
                                                    correctSlotY

                                            val distance =
                                                sqrt(
                                                    dx * dx +
                                                        dy * dy
                                                )

                                            if (
                                                distance <=
                                                snapThresholdPx
                                            ) {

                                                pieceOffsets =
                                                    pieceOffsets
                                                        .toMutableList()
                                                        .also {
                                                            it[index] =
                                                                Offset(
                                                                    correctSlotX,
                                                                    correctSlotY
                                                                )
                                                        }

                                                placed =
                                                    placed
                                                        .toMutableList()
                                                        .also {
                                                            it[index] =
                                                                true
                                                        }

                                                if (
                                                    placed.all {
                                                        it
                                                    }
                                                ) {
                                                    isSolved =
                                                        true
                                                }
                                            }
                                        }
                                    )
                                }
                    )
                }
            }
        }

        if (isSolved) {

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            Text(
                text =
                    "🎉 Puzzle Solved!",
                style =
                    MaterialTheme.typography
                        .headlineSmall,
                color =
                    Color.White
            )
        }
    }
}

private fun buildPieceBitmap(
    source: Bitmap,
    row: Int,
    col: Int,
    columns: Int,
    rows: Int,
    marginPx: Int
): Bitmap {

    val cellSize =
        minOf(
            source.width / columns,
            source.height / rows
        )

    val canvasSize =
        cellSize +
            marginPx * 2

    val pieceBitmap =
        Bitmap.createBitmap(
            canvasSize,
            canvasSize,
            Bitmap.Config.ARGB_8888
        )

    val canvas =
        Canvas(pieceBitmap)

    val left =
        col * cellSize -
            marginPx

    val top =
        row * cellSize -
            marginPx

    val right =
        left +
            canvasSize

    val bottom =
        top +
            canvasSize

    val clampedLeft =
        left.coerceIn(
            0,
            source.width
        )

    val clampedTop =
        top.coerceIn(
            0,
            source.height
        )

    val clampedRight =
        right.coerceIn(
            0,
            source.width
        )

    val clampedBottom =
        bottom.coerceIn(
            0,
            source.height
        )

    if (
        clampedRight > clampedLeft &&
        clampedBottom > clampedTop
    ) {

        val srcRect =
            Rect(
                clampedLeft,
                clampedTop,
                clampedRight,
                clampedBottom
            )

        val dstRect =
            Rect(
                clampedLeft - left,
                clampedTop - top,
                clampedRight - left,
                clampedBottom - top
            )

        canvas.drawBitmap(
            source,
            srcRect,
            dstRect,
            null
        )
    }

    return pieceBitmap
}

private fun buildPiecePath(
    size: Size,
    top: Int,
    right: Int,
    bottom: Int,
    left: Int
): Path {

    val margin =
        minOf(
            size.width,
            size.height
        ) *
            MARGIN_FRACTION /
            (
                1f +
                    2f *
                    MARGIN_FRACTION
            )

    val cell =
        minOf(
            size.width,
            size.height
        ) -
            margin * 2f

    val bump =
        cell *
            BUMP_FRACTION

    val t1 =
        0.35f

    val t2 =
        0.65f

    val lx =
        margin

    val ty =
        margin

    val rx =
        margin +
            cell

    val by =
        margin +
            cell

    val path =
        Path()

    path.moveTo(
        lx,
        ty
    )

    if (top == 0) {

        path.lineTo(
            rx,
            ty
        )

    } else {

        val x1 =
            lx +
                cell * t1

        val x2 =
            lx +
                cell * t2

        val midX =
            (x1 + x2) / 2f

        val bumpY =
            ty -
                top * bump

        path.lineTo(
            x1,
            ty
        )

        path.quadraticBezierTo(
            x1,
            bumpY,
            midX,
            bumpY
        )

        path.quadraticBezierTo(
            x2,
            bumpY,
            x2,
            ty
        )

        path.lineTo(
            rx,
            ty
        )
    }

    if (right == 0) {

        path.lineTo(
            rx,
            by
        )

    } else {

        val y1 =
            ty +
                cell * t1

        val y2 =
            ty +
                cell * t2

        val midY =
            (y1 + y2) / 2f

        val bumpX =
            rx +
                right * bump

        path.lineTo(
            rx,
            y1
        )

        path.quadraticBezierTo(
            bumpX,
            y1,
            bumpX,
            midY
        )

        path.quadraticBezierTo(
            bumpX,
            y2,
            rx,
            y2
        )

        path.lineTo(
            rx,
            by
        )
    }

    if (bottom == 0) {

        path.lineTo(
            lx,
            by
        )

    } else {

        val x1 =
            rx -
                cell * t1

        val x2 =
            rx -
                cell * t2

        val midX =
            (x1 + x2) / 2f

        val bumpY =
            by +
                bottom * bump

        path.lineTo(
            x1,
            by
        )

        path.quadraticBezierTo(
            x1,
            bumpY,
            midX,
            bumpY
        )

        path.quadraticBezierTo(
            x2,
            bumpY,
            x2,
            by
        )

        path.lineTo(
            lx,
            by
        )
    }

    if (left == 0) {

        path.lineTo(
            lx,
            ty
        )

    } else {

        val y1 =
            by -
                cell * t1

        val y2 =
            by -
                cell * t2

        val midY =
            (y1 + y2) / 2f

        val bumpX =
            lx -
                left * bump

        path.lineTo(
            lx,
            y1
        )

        path.quadraticBezierTo(
            bumpX,
            y1,
            bumpX,
            midY
        )

        path.quadraticBezierTo(
            bumpX,
            y2,
            lx,
            y2
        )

        path.lineTo(
            lx,
            ty
        )
    }

    path.close()

    return path
}