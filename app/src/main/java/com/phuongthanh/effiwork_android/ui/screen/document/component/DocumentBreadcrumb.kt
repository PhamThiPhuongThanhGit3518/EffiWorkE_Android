package com.phuongthanh.effiwork_android.ui.screen.document.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Surface
import com.phuongthanh.effiwork_android.viewmodel.document.BreadcrumbItem

@Composable
fun DocumentBreadcrumb(
    items: List<BreadcrumbItem>,
    modifier: Modifier = Modifier,
    onItemClick: (String) -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, item ->
            Text(
                text = item.label,
                modifier = Modifier
                    .clickable(enabled = !item.isCurrent) { onItemClick(item.id) },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (item.isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                color = if (item.isCurrent) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (index < items.size - 1) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(16.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Document Breadcrumb")
@Composable
fun DocumentBreadcrumbPreview() {
    MaterialTheme {
        Surface {
            DocumentBreadcrumb(
                items = listOf(
                    BreadcrumbItem(id = "root", label = "Tài liệu dự án", isCurrent = false),
                    BreadcrumbItem(id = "section-1", label = "Phần 1: Backend", isCurrent = false),
                    BreadcrumbItem(id = "task-99", label = "Thiết kế API", isCurrent = true)
                ),
                onItemClick = {}
            )
        }
    }
}
