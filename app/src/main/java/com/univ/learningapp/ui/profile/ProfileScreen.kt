package com.univ.learningapp.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.univ.learningapp.data.model.UserRole
import com.univ.learningapp.ui.theme.SuccessGreen
import com.univ.learningapp.ui.theme.WarningOrange

@Composable
fun ProfileTabContent(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val user by viewModel.currentUser.collectAsState(initial = viewModel.initialUser)
    val saveState by viewModel.saveState.collectAsState()

    val currentUser = user ?: return

    var phoneNumber by remember(currentUser.id) { mutableStateOf(currentUser.phoneNumber) }
    var instagram by remember(currentUser.id) { mutableStateOf(currentUser.instagramHandle) }
    var twitter by remember(currentUser.id) { mutableStateOf(currentUser.twitterHandle) }
    var linkedin by remember(currentUser.id) { mutableStateOf(currentUser.linkedinHandle) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.uploadPhoto(uri)
        }
    }

    LaunchedEffect(saveState) {
        if (saveState is ProfileSaveState.Success) {
            kotlinx.coroutines.delay(2000)
            viewModel.resetState()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(if (currentUser.role == UserRole.ADMIN || currentUser.role == UserRole.SUPER_ADMIN) WarningOrange else MaterialTheme.colorScheme.primary)
                .clickable {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (currentUser.photoUrl.isNotBlank()) {
                AsyncImage(
                    model = currentUser.photoUrl,
                    contentDescription = "Profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = currentUser.name.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Change photo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Text(text = "Tap the photo to change it", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = currentUser.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Text(text = currentUser.email, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))

                Surface(
                    color = if (currentUser.role == UserRole.ADMIN || currentUser.role == UserRole.SUPER_ADMIN)
                        WarningOrange.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = currentUser.role.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (currentUser.role == UserRole.ADMIN || currentUser.role == UserRole.SUPER_ADMIN)
                            WarningOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Contact & Social Links", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = instagram,
                    onValueChange = { instagram = it },
                    label = { Text("Instagram Handle") },
                    placeholder = { Text("@yourhandle") },
                    leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = twitter,
                    onValueChange = { twitter = it },
                    label = { Text("Twitter / X Handle") },
                    placeholder = { Text("@yourhandle") },
                    leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = linkedin,
                    onValueChange = { linkedin = it },
                    label = { Text("LinkedIn Handle / URL") },
                    leadingIcon = { Icon(Icons.Default.Language, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                when (val state = saveState) {
                    is ProfileSaveState.Success -> Text(state.message, color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    is ProfileSaveState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    else -> {}
                }

                Button(
                    onClick = { viewModel.saveProfile(phoneNumber, instagram, twitter, linkedin) },
                    enabled = saveState !is ProfileSaveState.Saving,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (saveState is ProfileSaveState.Saving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Save Profile", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
