package com.phuongthanh.effiwork_android.ui.common

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.phuongthanh.effiwork_android.ui.navigation.BottomNavItem

@Composable
fun BottomNavigationBar(
    items: List<BottomNavItem>,
    onItemClick: (BottomNavItem) -> Unit,
    navController: NavController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onItemClick(item) },
                icon = {
                    Icon(
                        imageVector = if (selected) getSelectedIcon(item) else getUnselectedIcon(item),
                        contentDescription = stringResource(item.labelResId),
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(text = stringResource(item.labelResId))
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

private fun getSelectedIcon(item: BottomNavItem): ImageVector {
    return when (item) {
        BottomNavItem.Projects -> Icons.Filled.Person
        BottomNavItem.Notifications -> Icons.Filled.Notifications
        BottomNavItem.Profile -> Icons.Outlined.AccountCircle
    }
}

private fun getUnselectedIcon(item: BottomNavItem): ImageVector {
    return when (item) {
        BottomNavItem.Projects -> Icons.Outlined.AccountCircle
        BottomNavItem.Notifications -> Icons.Outlined.Notifications
        BottomNavItem.Profile -> Icons.Outlined.AccountCircle
    }
}
