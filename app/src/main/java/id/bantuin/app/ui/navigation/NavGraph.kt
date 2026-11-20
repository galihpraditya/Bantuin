package id.bantuin.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import id.bantuin.app.data.repository.FakeBantuinRepository
import id.bantuin.app.ui.auth.LoginScreen
import id.bantuin.app.ui.auth.RegisterScreen
import id.bantuin.app.ui.common.BantuinBottomBar
import id.bantuin.app.ui.customer.OrderFlowManager
import id.bantuin.app.ui.customer.estimate.EstimateScreen
import id.bantuin.app.ui.customer.home.CustomerHomeScreen
import id.bantuin.app.ui.customer.orderdetail.OrderDetailScreen
import id.bantuin.app.ui.customer.orders.CustomerOrdersScreen
import id.bantuin.app.ui.customer.profile.CustomerProfileScreen
import id.bantuin.app.ui.customer.providerdetail.ProviderDetailScreen
import id.bantuin.app.ui.customer.recommendation.RecommendationScreen
import id.bantuin.app.ui.customer.schedule.ScheduleScreen
import id.bantuin.app.ui.provider.calendar.ProviderCalendarScreen
import id.bantuin.app.ui.provider.dashboard.ProviderDashboardScreen
import id.bantuin.app.ui.provider.evaluation.ProviderEvaluationScreen
import id.bantuin.app.ui.provider.home.ProviderHomeScreen
import id.bantuin.app.ui.provider.orders.ProviderOrdersScreen
import id.bantuin.app.ui.provider.profile.ProviderProfileEditScreen
import id.bantuin.app.ui.provider.services.ProviderServicesScreen

@Composable
fun BantuinNavGraph(
    repository: FakeBantuinRepository,
    navController: NavHostController = rememberNavController(),
    orderFlow: OrderFlowManager = remember { OrderFlowManager() },
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val activeRole by repository.activeRole().collectAsState(initial = "customer")

    val bottomBarRoutes = setOf(
        Routes.CUSTOMER_HOME, Routes.CUSTOMER_ORDERS, Routes.CUSTOMER_SCHEDULE, Routes.CUSTOMER_PROFILE,
        Routes.PROVIDER_HOME, Routes.PROVIDER_ORDERS, Routes.PROVIDER_CALENDAR, Routes.PROVIDER_DASHBOARD
    )
    val showBottomBar = currentRoute in bottomBarRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BantuinBottomBar(
                    currentRoute = currentRoute,
                    activeRole = activeRole,
                    onNavigateToRoute = { route ->
                        navController.navigate(route) {
                            popUpTo(if (activeRole == "provider") Routes.PROVIDER_HOME else Routes.CUSTOMER_HOME) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.CUSTOMER_HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Auth Routes
            composable(Routes.LOGIN) {
                LoginScreen(
                    repository = repository,
                    onLoginSuccess = {
                        navController.navigate(Routes.CUSTOMER_HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(Routes.REGISTER)
                    }
                )
            }

            composable(Routes.REGISTER) {
                RegisterScreen(
                    repository = repository,
                    onRegisterSuccess = {
                        navController.navigate(Routes.CUSTOMER_HOME) {
                            popUpTo(Routes.REGISTER) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.popBackStack()
                    }
                )
            }

            // Pelanggan Flow
            composable(Routes.CUSTOMER_HOME) {
                CustomerHomeScreen(
                    orderFlow = orderFlow,
                    onCategoryClick = { catId ->
                        orderFlow.selectedCategoryId = catId
                        navController.navigate(Routes.recommendation(catId))
                    },
                    onSwitchToProvider = {
                        navController.navigate(Routes.PROVIDER_HOME) {
                            popUpTo(Routes.CUSTOMER_HOME) { inclusive = true }
                        }
                    },
                    onEmergencyClick = {
                        orderFlow.selectedCategoryId = 2 // AC & Listrik
                        orderFlow.isUrgent = true
                        navController.navigate(Routes.recommendation(2))
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
                    onNavigateBack = { navController.popBackStack() },
                    onSelectProvider = { provider ->
                        orderFlow.selectedProvider = provider
                        navController.navigate(Routes.providerDetail(provider.providerId))
                    }
                )
            }

            composable(
                route = Routes.PROVIDER_DETAIL,
                arguments = listOf(navArgument("providerId") { type = NavType.StringType })
            ) { backStackEntry ->
                val providerId = backStackEntry.arguments?.getString("providerId") ?: "p01"
                ProviderDetailScreen(
                    providerId = providerId,
                    orderFlow = orderFlow,
                    onNavigateBack = { navController.popBackStack() },
                    onServiceSelected = { service ->
                        orderFlow.selectedService = service
                        navController.navigate(Routes.SCHEDULE_SELECT)
                    }
                )
            }

            composable(Routes.SCHEDULE_SELECT) {
                ScheduleScreen(
                    orderFlow = orderFlow,
                    repository = repository,
                    onNavigateBack = { navController.popBackStack() },
                    onContinueToEstimate = {
                        navController.navigate(Routes.ESTIMATE_ORDER)
                    }
                )
            }

            composable(Routes.ESTIMATE_ORDER) {
                EstimateScreen(
                    orderFlow = orderFlow,
                    repository = repository,
                    onNavigateBack = { navController.popBackStack() },
                    onOrderConfirmed = { bookingId ->
                        navController.navigate(Routes.orderTracking(bookingId)) {
                            popUpTo(Routes.CUSTOMER_HOME)
                        }
                    }
                )
            }

            composable(
                route = Routes.ORDER_TRACKING,
                arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
            ) { backStackEntry ->
                val bookingId = backStackEntry.arguments?.getString("bookingId") ?: "b01"
                OrderDetailScreen(
                    bookingId = bookingId,
                    repository = repository,
                    onNavigateBack = {
                        navController.navigate(Routes.CUSTOMER_HOME) {
                            popUpTo(Routes.CUSTOMER_HOME) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.CUSTOMER_ORDERS) {
                CustomerOrdersScreen(
                    repository = repository,
                    onOrderClick = { bookingId ->
                        navController.navigate(Routes.orderTracking(bookingId))
                    }
                )
            }

            composable(Routes.CUSTOMER_SCHEDULE) {
                CustomerOrdersScreen(
                    repository = repository,
                    onOrderClick = { bookingId ->
                        navController.navigate(Routes.orderTracking(bookingId))
                    }
                )
            }

            composable(Routes.CUSTOMER_PROFILE) {
                CustomerProfileScreen(
                    repository = repository,
                    onSwitchRole = {
                        navController.navigate(Routes.PROVIDER_HOME) {
                            popUpTo(Routes.CUSTOMER_HOME) { inclusive = true }
                        }
                    },
                    onSignOut = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // Mitra Flow
            composable(Routes.PROVIDER_HOME) {
                ProviderHomeScreen(
                    repository = repository,
                    onNavigateToRoute = { route -> navController.navigate(route) },
                    onSwitchToCustomer = {
                        navController.navigate(Routes.CUSTOMER_HOME) {
                            popUpTo(Routes.PROVIDER_HOME) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.PROVIDER_ORDERS) {
                ProviderOrdersScreen(repository = repository)
            }

            composable(Routes.PROVIDER_CALENDAR) {
                ProviderCalendarScreen(repository = repository)
            }

            composable(Routes.PROVIDER_DASHBOARD) {
                ProviderDashboardScreen(
                    onNavigateToEvaluation = {
                        navController.navigate(Routes.PROVIDER_EVALUATION)
                    }
                )
            }

            composable(Routes.PROVIDER_EVALUATION) {
                ProviderEvaluationScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Routes.PROVIDER_SERVICES) {
                ProviderServicesScreen(repository = repository)
            }

            composable(Routes.PROVIDER_PROFILE_EDIT) {
                ProviderProfileEditScreen(
                    repository = repository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
