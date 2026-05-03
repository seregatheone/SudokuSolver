package pet.project.sudokusolver

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform