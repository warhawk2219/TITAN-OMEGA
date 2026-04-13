package com.irongate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irongate.model.FeedEntry
import com.irongate.ui.components.*
import com.irongate.ui.theme.IronColors

@Composable
fun LogsScreen(feed: List<FeedEntry>) {
    val listState = rememberLazyListState()

    LaunchedEffect(feed.size) {
        if (feed.isNotEmpty()) listState.animateScrollToItem(0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IronColors.Black)
            .padding(16.dp)
    ) {
        SectionTitle("MESSAGE FEED")

        if (feed.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                MonoText("NO MESSAGES YET", fontSize = 10.sp, color = IronColors.Gray1)
            }
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(feed, key = { it.id }) { entry ->
                    FeedCard(entry)
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun FeedCard(entry: FeedEntry) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(IronColors.NearBlack)
            .border(1.dp, IronColors.Dark2)
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FeedTag(entry.type)
                MonoText(entry.time, fontSize = 9.sp, color = IronColors.Gray1)
            }
            Spacer(Modifier.height(6.dp))
            MonoText(entry.summary, fontSize = 10.sp, color = IronColors.Gray3)
            Spacer(Modifier.height(4.dp))
            MonoText(entry.route, fontSize = 9.sp, color = IronColors.Gray1)
        }
    }
}
