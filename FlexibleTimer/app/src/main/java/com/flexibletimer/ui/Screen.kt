package com.flexibletimer.ui

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object SequentialMenu : Screen("sequential_menu")
    object SequentialNew : Screen("sequential_new")
    object SequentialSaved : Screen("sequential_saved")
    object GroupMenu : Screen("group_menu")
    object GroupNew : Screen("group_new")
    object GroupSaved : Screen("group_saved")
    object RunningSequential : Screen("running_sequential")
    object RunningGroup : Screen("running_group")
}
