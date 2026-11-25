package id.bantuin.app.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import id.bantuin.app.ui.theme.BantuinMotion
import id.bantuin.app.ui.theme.rememberReducedMotion
import id.bantuin.app.core.settings.InMemorySettingsStore
import id.bantuin.app.core.settings.SettingsPreferenceStore
import id.bantuin.app.core.theme.InMemoryThemeStore
import id.bantuin.app.core.theme.ThemePreferenceStore
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.domain.model.PaymentMethod
import id.bantuin.app.domain.repository.BantuinRepository
import id.bantuin.app.domain.repository.UserProfile
import id.bantuin.app.ui.auth.LoginScreen
import id.bantuin.app.ui.auth.RegisterScreen
import id.bantuin.app.ui.chat.ChatListScreen
import id.bantuin.app.ui.chat.ChatRoomScreen
import id.bantuin.app.ui.common.BantuinBottomBar
import id.bantuin.app.ui.common.bottomBarRoutes
import id.bantuin.app.ui.common.customerNavTabs
import id.bantuin.app.ui.common.providerNavTabs
import id.bantuin.app.ui.customer.OrderFlowManager
import id.bantuin.app.ui.customer.OrderFlowViewModel
import id.bantuin.app.ui.customer.estimate.EstimateScreen
import id.bantuin.app.ui.customer.home.CustomerHomeScreen
import id.bantuin.app.ui.customer.notifications.CustomerNotificationsScreen
import id.bantuin.app.ui.customer.orderdetail.OrderDetailScreen
import id.bantuin.app.ui.customer.orders.CustomerOrdersScreen
import id.bantuin.app.ui.customer.profile.CustomerProfileScreen
import id.bantuin.app.ui.customer.providerdetail.ProviderDetailScreen
import id.bantuin.app.ui.customer.recommendation.RecommendationScreen
import id.bantuin.app.ui.customer.schedule.ScheduleScreen
import id.bantuin.app.ui.payment.QrisPaymentScreen
import id.bantuin.app.ui.provider.calendar.ProviderCalendarScreen
import id.bantuin.app.ui.provider.dashboard.ProviderDashboardScreen
import id.bantuin.app.ui.provider.evaluation.ProviderEvaluationScreen
import id.bantuin.app.ui.provider.home.ProviderHomeScreen
import id.bantuin.app.ui.provider.orders.CompleteOrderConfirmScreen
import id.bantuin.app.ui.provider.orders.ProviderOrdersScreen
import id.bantuin.app.ui.provider.profile.ProviderProfileEditScreen
import id.bantuin.app.ui.provider.profile.ProviderProfileScreen
import id.bantuin.app.ui.provider.services.ProviderServicesScreen
import id.bantuin.app.ui.provider.wallet.ProviderWithdrawScreen
import id.bantuin.app.ui.settings.SettingsScreen

@Composable
fun BantuinNavGraph(
    repository: BantuinRepository,
    themeStore: ThemePreferenceStore = remember { InMemoryThemeStore() },
    settingsStore: SettingsPreferenceStore = remember { InMemorySettingsStore() },
    initialUser: UserProfile? = null,
    initialRole: String = "customer",
    navController: NavHostController = rememberNavController(),
    orderFlowViewModel: OrderFlowViewModel = viewModel(),
    orderFlow: OrderFlowManager = orderFlowViewModel.flow,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val activeRole by repository.activeRole().collectAsState(initial = initialRole)
    val startDestination = when {
        initialUser == null -> Routes.LOGIN
        initialRole == "provider" -> Routes.PROVIDER_HOME
        else -> Routes.CUSTOMER_HOME
    }
    val customerBookings by repository.observeCustomerBookings().collectAsState(initial = emptyList())
    val chatThreads by repository.observeChatThreads(activeRole).collectAsState(initial = emptyList())
    val unreadChatCount = chatThreads.sumOf { it.unreadCount }
    val hasActiveCustomerOrders = customerBookings.any {
        it.status in listOf(
            BookingStatus.PENDING,
            BookingStatus.DIKONFIRMASI,
            BookingStatus.DALAM_PENGERJAAN
        )
    }

    val activeBottomBarRoutes = remember(activeRole) { activeBottomBarRoutesFor(activeRole) }
    val showBottomBar = currentRoute in activeBottomBarRoutes
    val reducedMotion = rememberReducedMotion()
    val modalRoutes = remember {
        setOf(
            Routes.QRIS_PAYMENT,
            Routes.COMPLETE_ORDER,
            Routes.PROVIDER_WITHDRAW,
            Routes.SETTINGS
        )
    }

    fun hasBackStackEntry(route: String): Boolean = try {
        navController.getBackStackEntry(route)
        true
    } catch (_: IllegalArgumentException) {
        false
    }

    fun navigateToTab(route: String) {
        val currentEntry = navController.currentBackStackEntry
        if (currentEntry != null && !currentEntry.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            return
        }
        if (navController.currentDestination?.route == route) return
        val homeRoute = if (activeRole == "provider") Routes.PROVIDER_HOME else Routes.CUSTOMER_HOME
        val hasHomeInBackStack = hasBackStackEntry(homeRoute)
        navController.navigate(route) {
            if (hasHomeInBackStack) {
                popUpTo(homeRoute) {
                    saveState = true
                }
            } else {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateSingleTop(route: String) {
        val currentEntry = navController.currentBackStackEntry
        if (currentEntry != null && !currentEntry.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            return
        }
        navController.navigate(route) {
            launchSingleTop = true
        }
    }

    fun safePopBackStack(fallbackRoute: String? = null) {
        val currentEntry = navController.currentBackStackEntry
        if (currentEntry != null && !currentEntry.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            return
        }
        val popped = navController.popBackStack()
        if (!popped && fallbackRoute != null) {
            navController.navigate(fallbackRoute) {
                launchSingleTop = true
            }
        }
    }

    fun resetAndNavigate(route: String) {
        bottomBarRoutes().forEach { tabRoute ->
            navController.clearBackStack(tabRoute)
        }
        navController.navigate(route) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun navigateReplacingOrderFlow(route: String) {
        val hasCustomerHome = hasBackStackEntry(Routes.CUSTOMER_HOME)
        navController.navigate(route) {
            if (hasCustomerHome) {
                popUpTo(Routes.CUSTOMER_HOME)
            } else {
                popUpTo(navController.graph.findStartDestination().id)
            }
            launchSingleTop = true
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BantuinBottomBar(
                    currentRoute = currentRoute,
                    activeRole = activeRole,
                    unreadChatCount = unreadChatCount,
                    onNavigateToRoute = ::navigateToTab
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = {
                if (reducedMotion) {
                    EnterTransition.None
                } else {
                    val from = initialState.destination.route
                    val to = targetState.destination.route
                    when {
                        from in activeBottomBarRoutes && to in activeBottomBarRoutes ->
                            fadeIn(tween(BantuinMotion.TAB_FADE_IN_MS))
                        to in modalRoutes ->
                            slideInVertically(tween(BantuinMotion.ENTER_MS)) { it / 5 } +
                                fadeIn(tween(BantuinMotion.ENTER_MS))
                        else ->
                            slideInHorizontally(tween(BantuinMotion.ENTER_MS)) { it / 5 } +
                                fadeIn(tween(BantuinMotion.ENTER_MS))
                    }
                }
            },
            exitTransition = {
                if (reducedMotion) {
                    ExitTransition.None
                } else {
                    val from = initialState.destination.route
                    val to = targetState.destination.route
                    when {
                        from in activeBottomBarRoutes && to in activeBottomBarRoutes ->
                            fadeOut(tween(BantuinMotion.TAB_FADE_OUT_MS))
                        to in modalRoutes ->
                            fadeOut(tween(BantuinMotion.EXIT_MS))
                        else ->
                            slideOutHorizontally(tween(BantuinMotion.EXIT_MS)) { -it / 6 } +
                                fadeOut(tween(BantuinMotion.EXIT_MS))
                    }
                }
            },
            popEnterTransition = {
                if (reducedMotion) {
                    EnterTransition.None
                } else {
                    val from = initialState.destination.route
                    val to = targetState.destination.route
                    when {
                        from in activeBottomBarRoutes && to in activeBottomBarRoutes ->
                            fadeIn(tween(BantuinMotion.TAB_FADE_IN_MS))
                        from in modalRoutes ->
                            fadeIn(tween(BantuinMotion.ENTER_MS))
                        else ->
                            slideInHorizontally(tween(BantuinMotion.ENTER_MS)) { -it / 6 } +
                                fadeIn(tween(BantuinMotion.ENTER_MS))
                    }
                }
            },
            popExitTransition = {
                if (reducedMotion) {
                    ExitTransition.None
                } else {
                    val from = initialState.destination.route
                    val to = targetState.destination.route
                    when {
                        from in activeBottomBarRoutes && to in activeBottomBarRoutes ->
                            fadeOut(tween(BantuinMotion.TAB_FADE_OUT_MS))
                        from in modalRoutes ->
                            slideOutVertically(tween(BantuinMotion.EXIT_MS)) { it / 5 } +
                                fadeOut(tween(BantuinMotion.EXIT_MS))
                        else ->
                            slideOutHorizontally(tween(BantuinMotion.EXIT_MS)) { it / 5 } +
                                fadeOut(tween(BantuinMotion.EXIT_MS))
                    }
                }
            },
            modifier = Modifier.padding(innerPadding)
        ) {
            // Auth Routes
            composable(Routes.LOGIN) {
                LoginScreen(
                    repository = repository,
                    onLoginSuccess = { user ->
                        resetAndNavigate(homeRouteFor(user))
                    },
                    onNavigateToRegister = {
                        navigateSingleTop(Routes.REGISTER)
                    }
                )
            }

            composable(Routes.REGISTER) {
                RegisterScreen(
                    repository = repository,
                    onRegisterSuccess = { user, isProvider ->
                        val preferred = if (isProvider) "provider" else "customer"
                        resetAndNavigate(homeRouteFor(user, preferred))
                    },
                    onNavigateToLogin = {
                        safePopBackStack()
                    }
                )
            }

            // Pelanggan Flow
            composable(Routes.CUSTOMER_HOME) {
                CustomerHomeScreen(
                    orderFlow = orderFlow,
                    repository = repository,
                    hasUnreadNotifications = hasActiveCustomerOrders,
                    onCategoryClick = { catId ->
                        orderFlow.selectedCategoryId = catId
                        navigateSingleTop(Routes.recommendation(catId))
                    },
                    onProviderClick = { providerId ->
                        navigateSingleTop(Routes.providerDetail(providerId))
                    },
                    onNotificationsClick = {
                        navigateSingleTop(Routes.CUSTOMER_NOTIFICATIONS)
                    },
                    onActiveOrderClick = { bookingId ->
                        navigateSingleTop(Routes.orderTracking(bookingId))
                    },
                    onEmergencyClick = {
                        orderFlow.selectedCategoryId = 2 // AC & Listrik
                        orderFlow.isUrgent = true
                        navigateSingleTop(Routes.recommendation(2))
                    }
                )
            }

            composable(
                route = Routes.RECOMMENDATION,
                arguments = listOf(navArgument("categoryId") { type = NavType.IntType })
            ) { backStackEntry ->
                val categoryId = backStackEntry.arguments?.getInt("categoryId") ?: 2
                RecommendationScreen(
                    categoryId = categoryId,
                    orderFlow = orderFlow,
                    repository = repository,
                    onNavigateBack = { safePopBackStack() },
                    onSelectProvider = { provider ->
                        orderFlow.selectedProvider = provider
                        navigateSingleTop(Routes.providerDetail(provider.providerId))
                    }
                )
            }

            composable(
                route = Routes.PROVIDER_DETAIL,
                arguments = listOf(navArgument("providerId") { type = NavType.StringType })
            ) { backStackEntry ->
                val providerId = backStackEntry.arguments?.getString("providerId").orEmpty()
                ProviderDetailScreen(
                    providerId = providerId,
                    orderFlow = orderFlow,
                    repository = repository,
                    onNavigateBack = { safePopBackStack() },
                    onServiceSelected = { service ->
                        orderFlow.selectedService = service
                        navigateSingleTop(Routes.SCHEDULE_SELECT)
                    }
                )
            }

            composable(Routes.SCHEDULE_SELECT) {
                ScheduleScreen(
                    orderFlow = orderFlow,
                    repository = repository,
                    onNavigateBack = { safePopBackStack() },
                    onContinueToEstimate = {
                        navigateSingleTop(Routes.ESTIMATE_ORDER)
                    }
                )
            }

            composable(Routes.ESTIMATE_ORDER) {
                EstimateScreen(
                    orderFlow = orderFlow,
                    repository = repository,
                    onNavigateBack = { safePopBackStack() },
                    onOrderConfirmed = { bookingId, paymentMethod ->
                        val targetRoute = if (paymentMethod == PaymentMethod.QRIS) {
                            Routes.qrisPayment(bookingId)
                        } else {
                            Routes.orderTracking(bookingId)
                        }
                        navigateReplacingOrderFlow(targetRoute)
                    }
                )
            }

            composable(
                route = Routes.ORDER_TRACKING,
                arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId").orEmpty()
                OrderDetailScreen(
                    bookingId = bookingId,
                    repository = repository,
                    onNavigateBack = {
                        safePopBackStack(fallbackRoute = Routes.CUSTOMER_HOME)
                    },
                    onOpenChat = { chatBookingId ->
                        navigateSingleTop(Routes.chatRoom(chatBookingId))
                    },
                    onOpenQrisPayment = { payBookingId ->
                        navigateSingleTop(Routes.qrisPayment(payBookingId))
                    }
                )
            }

            composable(Routes.CUSTOMER_ORDERS) {
                CustomerOrdersScreen(
                    repository = repository,
                    onOrderClick = { bookingId ->
                        navigateSingleTop(Routes.orderTracking(bookingId))
                    },
                    onOpenQrisPayment = { bookingId ->
                        navigateSingleTop(Routes.qrisPayment(bookingId))
                    }
                )
            }

            composable(Routes.CUSTOMER_NOTIFICATIONS) {
                CustomerNotificationsScreen(
                    repository = repository,
                    canNavigateBack = true,
                    onNavigateBack = { safePopBackStack() },
                    onOrderClick = { bookingId ->
                        navigateSingleTop(Routes.orderTracking(bookingId))
                    }
                )
            }

            composable(Routes.CHAT_LIST) {
                ChatListScreen(
                    repository = repository,
                    canNavigateBack = activeRole == "provider",
                    onNavigateBack = { safePopBackStack() },
                    onThreadClick = { bookingId ->
                        navigateSingleTop(Routes.chatRoom(bookingId))
                    }
                )
            }

            composable(
                route = Routes.CHAT_ROOM,
                arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId").orEmpty()
                ChatRoomScreen(
                    bookingId = bookingId,
                    repository = repository,
                    onNavigateBack = { safePopBackStack() }
                )
            }

            composable(
                route = Routes.QRIS_PAYMENT,
                arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId").orEmpty()
                QrisPaymentScreen(
                    bookingId = bookingId,
                    repository = repository,
                    onNavigateBack = { safePopBackStack() },
                    onViewOrderDetail = { targetId ->
                        navigateReplacingOrderFlow(Routes.orderTracking(targetId))
                    }
                )
            }

            composable(Routes.CUSTOMER_PROFILE) {
                CustomerProfileScreen(
                    repository = repository,
                    onOpenSettings = {
                        navigateSingleTop(Routes.SETTINGS)
                    },
                    onOpenOrders = {
                        navigateToTab(Routes.CUSTOMER_ORDERS)
                    },
                    onOpenNotifications = {
                        navigateSingleTop(Routes.CUSTOMER_NOTIFICATIONS)
                    }
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    repository = repository,
                    themeStore = themeStore,
                    settingsStore = settingsStore,
                    onNavigateBack = { safePopBackStack() },
                    onSwitchedToProvider = {
                        resetAndNavigate(Routes.PROVIDER_HOME)
                    },
                    onSwitchedToCustomer = {
                        resetAndNavigate(Routes.CUSTOMER_HOME)
                    },
                    onSignOut = {
                        orderFlow.resetAll()
                        resetAndNavigate(Routes.LOGIN)
                    }
                )
            }

            composable(Routes.PROVIDER_PROFILE) {
                ProviderProfileScreen(
                    repository = repository,
                    onOpenSettings = {
                        navigateSingleTop(Routes.SETTINGS)
                    },
                    onNavigateToEditProfile = {
                        navigateSingleTop(Routes.PROVIDER_PROFILE_EDIT)
                    },
                    onNavigateToServices = {
                        navigateSingleTop(Routes.PROVIDER_SERVICES)
                    },
                    onNavigateToCalendar = {
                        navigateSingleTop(Routes.PROVIDER_CALENDAR)
                    },
                    onNavigateToEvaluation = {
                        navigateSingleTop(Routes.PROVIDER_EVALUATION)
                    }
                )
            }

            // Mitra Flow
            composable(Routes.PROVIDER_HOME) {
                ProviderHomeScreen(
                    repository = repository,
                    onNavigateToRoute = { route ->
                        if (route in activeBottomBarRoutes) {
                            navigateToTab(route)
                        } else {
                            navigateSingleTop(route)
                        }
                    }
                )
            }

            composable(Routes.PROVIDER_ORDERS) {
                ProviderOrdersScreen(
                    repository = repository,
                    onOpenChat = { bookingId ->
                        navigateSingleTop(Routes.chatRoom(bookingId))
                    },
                    onOpenCompleteOrder = { bookingId ->
                        navigateSingleTop(Routes.completeOrder(bookingId))
                    }
                )
            }

            composable(Routes.PROVIDER_CALENDAR) {
                ProviderCalendarScreen(
                    repository = repository,
                    canNavigateBack = true,
                    onNavigateBack = { safePopBackStack() }
                )
            }

            composable(Routes.PROVIDER_DASHBOARD) {
                ProviderDashboardScreen(
                    repository = repository,
                    onNavigateToEvaluation = {
                        navigateSingleTop(Routes.PROVIDER_EVALUATION)
                    },
                    onOpenWithdraw = {
                        navigateSingleTop(Routes.PROVIDER_WITHDRAW)
                    }
                )
            }

            composable(Routes.PROVIDER_EVALUATION) {
                ProviderEvaluationScreen(
                    repository = repository,
                    onNavigateBack = { safePopBackStack() }
                )
            }

            composable(Routes.PROVIDER_SERVICES) {
                ProviderServicesScreen(
                    repository = repository,
                    onNavigateBack = { safePopBackStack() }
                )
            }

            composable(Routes.PROVIDER_PROFILE_EDIT) {
                ProviderProfileEditScreen(
                    repository = repository,
                    onNavigateBack = { safePopBackStack() }
                )
            }

            composable(
                route = Routes.COMPLETE_ORDER,
                arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId").orEmpty()
                CompleteOrderConfirmScreen(
                    bookingId = bookingId,
                    repository = repository,
                    onNavigateBack = { safePopBackStack() },
                    onCompleted = { safePopBackStack() }
                )
            }

            composable(Routes.PROVIDER_WITHDRAW) {
                ProviderWithdrawScreen(
                    repository = repository,
                    onNavigateBack = { safePopBackStack() }
                )
            }
        }
    }
}

internal fun activeBottomBarRoutesFor(activeRole: String): Set<String> =
    if (activeRole == "provider") {
        providerNavTabs().map { it.route }.toSet()
    } else {
        customerNavTabs().map { it.route }.toSet()
    }

internal fun homeRouteFor(user: UserProfile?, activeRole: String = "customer"): String {
    val roles = user?.roles.orEmpty()
    return when {
        activeRole == "provider" && "provider" in roles -> Routes.PROVIDER_HOME
        "provider" in roles && "customer" !in roles -> Routes.PROVIDER_HOME
        else -> Routes.CUSTOMER_HOME
    }
}
