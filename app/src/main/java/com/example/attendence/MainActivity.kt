// MainActivity.kt
package com.example.attendance


import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Cancel
import android.content.Intent
import android.content.Context
import android.app.Activity
import android.os.Handler
import android.os.Looper
// Update your theme import to match your actual package
import com.example.attendance.ui.theme.AttendanceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AttendanceApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceApp() {
    val context = LocalContext.current
    val viewModel: AttendanceViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    // Auto-login on app start if credentials are saved
    LaunchedEffect(Unit) {
        viewModel.autoLogin(context)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when {
            uiState.currentUser == null -> {
                AuthenticationScreen(viewModel)
            }
            else -> {
                MainScreen(viewModel, uiState)
            }
        }
    }
}

// Data Models
data class User(
    val id: String,
    val name: String,
    val email: String,
    val password: String,
    val role: UserRole
)

enum class UserRole { STUDENT, INSTRUCTOR }

data class Course(
    val id: String,
    val name: String,
    val code: String,
    val description: String,
    val instructorId: String,
    val schedule: String,
    val studentIds: List<String> = emptyList()
)

data class AttendanceRecord(
    val id: String,
    val courseId: String,
    val studentId: String,
    val date: String,
    val isPresent: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class AttendanceUiState(
    val currentUser: User? = null,
    val courses: List<Course> = emptyList(),
    val attendanceRecords: List<AttendanceRecord> = emptyList(),
    val students: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: String? = null
)

// ViewModel
class AttendanceViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AttendanceUiState())
    val uiState: StateFlow<AttendanceUiState> = _uiState.asStateFlow()

    // Mock database - In real app, use Room Database
    private val users = mutableListOf<User>()
    private val courses = mutableListOf<Course>()
    private val attendanceRecords = mutableListOf<AttendanceRecord>()

    // SharedPreferences keys
    private val PREF_NAME = "attendance_prefs"
    private val KEY_USER_EMAIL = "user_email"
    private val KEY_USER_PASSWORD = "user_password"
    private val KEY_REMEMBER_ME = "remember_me"

    init {
        // Initialize with sample data
        initializeSampleData()
    }

    private fun initializeSampleData() {
        // Sample users
        users.addAll(listOf(
            User("1", "Dr. Smith", "smith@university.edu", "somePassword", UserRole.INSTRUCTOR),
            User("2", "John Doe", "john@student.edu","somePassword", UserRole.STUDENT),
            User("3", "Jane Wilson", "jane@student.edu","somePassword", UserRole.STUDENT)
        ))

        // Sample courses
        courses.addAll(listOf(
            Course("1", "Computer Science 101", "CS101", "Introduction to Programming", "1", "MWF 10:00-11:00", listOf("2", "3")),
            Course("2", "Data Structures", "CS201", "Advanced Data Structures", "1", "TTh 2:00-3:30", listOf("2"))
        ))

        updateUiState()
    }

    // Auto-login function
    fun autoLogin(context: Context) {
        Log.d("AutoLogin", "Starting auto login")

        // Add this line to ensure sample data is loaded
        if (users.isEmpty()) {
            Log.d("AutoLogin", "Users list empty, initializing sample data")
            initializeSampleData()
            Log.d("AutoLogin", "Sample data initialized, users count: ${users.size}")
        }

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val rememberMe = prefs.getBoolean(KEY_REMEMBER_ME, false)
        Log.d("AutoLogin", "Remember me: $rememberMe")

        if (rememberMe) {
            val email = prefs.getString(KEY_USER_EMAIL, "") ?: ""
            val password = prefs.getString(KEY_USER_PASSWORD, "") ?: ""
            Log.d("AutoLogin", "Email: $email, Password length: ${password.length}")

            if (email.isNotEmpty() && password.isNotEmpty()) {
                Log.d("AutoLogin", "Attempting login")
                try {
                    // Perform login


                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        Log.d("AutoLogin", "Attempting login")
                        try {
                            login(email, password, false, context, false)
                            // Let login() function handle everything internally
                        } catch (e: Exception) {
                            Log.e("AutoLogin", "Login error: ${e.message}")
                        }
                    } else {
                        Log.w("AutoLogin", "Missing email or password for auto-login")
                    }
                } catch (e: Exception) {
                    Log.e("AutoLogin", "Login failed with exception", e)
                    // Clear stored credentials on error
                    clearStoredCredentials(context)
                }
            } else {
                Log.d("AutoLogin", "Email or password is empty")
            }
        } else {
            Log.d("AutoLogin", "Remember me is disabled")
        }
    }

    private fun navigateToMainScreen(context: Context) {
        try {
            // Replace YourMainActivity with your actual main activity class
            val intent = Intent(context, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)

            // Finish current activity if it's an Activity
            if (context is Activity) {
                context.finish()
            }
            Log.d("AutoLogin", "Navigation to main screen completed")
        } catch (e: Exception) {
            Log.e("AutoLogin", "Failed to navigate to main screen", e)
        }
    }

    private fun clearStoredCredentials(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().apply {
                remove(KEY_USER_EMAIL)
                remove(KEY_USER_PASSWORD)
                putBoolean(KEY_REMEMBER_ME, false)
                apply()
            }
            Log.d("AutoLogin", "Stored credentials cleared due to login failure")
        } catch (e: Exception) {
            Log.e("AutoLogin", "Failed to clear stored credentials", e)
        }
    }

    // Alternative version if your login function doesn't return boolean
    fun autoLoginAlternative(context: Context) {
        Log.d("AutoLogin", "Starting auto login")

        if (users.isEmpty()) {
            Log.d("AutoLogin", "Users list empty, initializing sample data")
            initializeSampleData()
            Log.d("AutoLogin", "Sample data initialized, users count: ${users.size}")
        }

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val rememberMe = prefs.getBoolean(KEY_REMEMBER_ME, false)
        Log.d("AutoLogin", "Remember me: $rememberMe")

        if (rememberMe) {
            val email = prefs.getString(KEY_USER_EMAIL, "") ?: ""
            val password = prefs.getString(KEY_USER_PASSWORD, "") ?: ""
            Log.d("AutoLogin", "Email: $email, Password length: ${password.length}")

            if (email.isNotEmpty() && password.isNotEmpty()) {
                Log.d("AutoLogin", "Attempting login")
                try {
                    // Check if user exists before calling login
                    val user = users.find { it.email == email && it.password == password }

                    if (user != null) {
                        Log.d("AutoLogin", "User found, performing login")
                        login(email, password, false, context, false)

                        // Add a small delay to ensure login completes
                        Handler(Looper.getMainLooper()).postDelayed({
                            navigateToMainScreen(context)
                        }, 100)
                    } else {
                        Log.w("AutoLogin", "User not found in database")
                        clearStoredCredentials(context)
                    }
                } catch (e: Exception) {
                    Log.e("AutoLogin", "Login failed with exception", e)
                    clearStoredCredentials(context)
                }
            } else {
                Log.d("AutoLogin", "Email or password is empty")
            }
        } else {
            Log.d("AutoLogin", "Remember me is disabled")
        }
    }

    fun login(email: String, password: String, rememberMe: Boolean = false, context: Context? = null, showSuccessMessage: Boolean = true) {
        if (email.isEmpty() || password.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Please fill in all fields")
            return
        }

        val user = users.find { it.email == email }
        if (user != null) {
            // Save credentials if remember me is checked
            if (rememberMe && context != null) {
                saveCredentials(context, email, password, true)
            } else if (context != null) {
                // Clear saved credentials if remember me is unchecked
                clearCredentials(context)
            }

            _uiState.value = _uiState.value.copy(
                currentUser = user,
                error = null,
                success = if (showSuccessMessage) "Login successful" else null
            )
            updateUiState()
        } else {
            _uiState.value = _uiState.value.copy(error = "Invalid credentials")
        }
    }

    fun register(name: String, email: String, password: String, role: UserRole) {
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Please fill in all fields")
            return
        }

        if (users.any { it.email == email }) {
            _uiState.value = _uiState.value.copy(error = "Email already exists")
            return
        }
        val password = "defaultPassword"
        val newUser = User(
            UUID.randomUUID().toString(),
            name,
            email,
            password,  // Add this line
            when(role.toString().uppercase()) {  // Convert string to UserRole enum
                "STUDENT" -> UserRole.STUDENT
                "INSTRUCTOR" -> UserRole.INSTRUCTOR
                else -> UserRole.STUDENT
            }
        )
        users.add(newUser)

        // Show success message and DON'T auto-login
        _uiState.value = _uiState.value.copy(
            currentUser = null, // Keep user logged out
            error = null,
            success = "Registration successful! Please login with your credentials."
        )
        updateUiState()
    }

    fun logout(context: Context? = null) {
        // Clear saved credentials on logout
        if (context != null) {
            clearCredentials(context)
        }
        _uiState.value = _uiState.value.copy(currentUser = null)
    }

    private fun saveCredentials(context: Context, email: String, password: String, rememberMe: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_PASSWORD, password)
            putBoolean(KEY_REMEMBER_ME, rememberMe)
            apply()
        }
    }

    private fun clearCredentials(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        with(prefs.edit()) {
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_PASSWORD)
            remove(KEY_REMEMBER_ME)
            apply()
        }
    }

    fun getSavedCredentials(context: Context): Triple<String, String, Boolean> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val email = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        val password = prefs.getString(KEY_USER_PASSWORD, "") ?: ""
        val rememberMe = prefs.getBoolean(KEY_REMEMBER_ME, false)
        return Triple(email, password, rememberMe)
    }

    fun addCourse(name: String, code: String, description: String, schedule: String) {
        val currentUser = _uiState.value.currentUser ?: return
        if (currentUser.role != UserRole.INSTRUCTOR) return

        val newCourse = Course(
            UUID.randomUUID().toString(),
            name, code, description, currentUser.id, schedule
        )
        courses.add(newCourse)
        updateUiState()
        _uiState.value = _uiState.value.copy(success = "Course added successfully")
    }

    fun markAttendance(courseId: String, isPresent: Boolean) {
        val currentUser = _uiState.value.currentUser ?: return
        if (currentUser.role != UserRole.STUDENT) return

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Remove existing record for today if any
        attendanceRecords.removeAll {
            it.courseId == courseId && it.studentId == currentUser.id && it.date == today
        }

        // Add new record
        val record = AttendanceRecord(
            UUID.randomUUID().toString(),
            courseId, currentUser.id, today, isPresent
        )
        attendanceRecords.add(record)
        updateUiState()
        _uiState.value = _uiState.value.copy(
            success = if (isPresent) "Marked as Present" else "Marked as Absent"
        )
    }

    private fun updateUiState() {
        val currentUser = _uiState.value.currentUser
        val userCourses = when (currentUser?.role) {
            UserRole.INSTRUCTOR -> courses.filter { it.instructorId == currentUser.id }
            UserRole.STUDENT -> courses.filter { currentUser.id in it.studentIds }
            null -> emptyList()
        }

        _uiState.value = _uiState.value.copy(
            courses = userCourses,
            attendanceRecords = attendanceRecords,
            students = users.filter { it.role == UserRole.STUDENT }
        )
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, success = null)
    }

    fun getTodayAttendance(courseId: String, studentId: String): AttendanceRecord? {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return attendanceRecords.find {
            it.courseId == courseId && it.studentId == studentId && it.date == today
        }
    }
}

// Authentication Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticationScreen(viewModel: AttendanceViewModel) {
    val context = LocalContext.current
    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf(TextFieldValue()) }
    var password by remember { mutableStateOf(TextFieldValue()) }
    var name by remember { mutableStateOf(TextFieldValue()) }
    var selectedRole by remember { mutableStateOf(UserRole.STUDENT) }
    var showPassword by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()

    // Load saved credentials when switching to login
    LaunchedEffect(isLogin) {
        if (isLogin) {
            val (savedEmail, savedPassword, savedRememberMe) = viewModel.getSavedCredentials(context)
            if (savedEmail.isNotEmpty()) {
                email = TextFieldValue(savedEmail)
                password = TextFieldValue(savedPassword)
                rememberMe = savedRememberMe
            }
        }
    }

    LaunchedEffect(uiState.error, uiState.success) {
        if (uiState.error != null || uiState.success != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessages()
        }
    }

    // Reset form when switching between login and register
    LaunchedEffect(isLogin) {
        if (!isLogin) {
            // Clear form when switching to register
            name = TextFieldValue()
            email = TextFieldValue()
            password = TextFieldValue()
            rememberMe = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "University Attendance",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Toggle between Login and Register
                Row {
                    TextButton(
                        onClick = { isLogin = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isLogin) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("Login", fontWeight = if (isLogin) FontWeight.Bold else FontWeight.Normal)
                    }

                    TextButton(
                        onClick = { isLogin = false },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (!isLogin) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("Register", fontWeight = if (!isLogin) FontWeight.Bold else FontWeight.Normal)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Registration fields
                if (!isLogin) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Role selection
                    Text("Select Role:", style = MaterialTheme.typography.bodyMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedRole == UserRole.STUDENT,
                                onClick = { selectedRole = UserRole.STUDENT }
                            )
                            Text("Student")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedRole == UserRole.INSTRUCTOR,
                                onClick = { selectedRole = UserRole.INSTRUCTOR }
                            )
                            Text("Instructor")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Remember Me checkbox (only for login)
                if (isLogin) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it }
                        )
                        Text(
                            text = "Remember me",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Error/Success messages
                uiState.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                uiState.success?.let { success ->
                    Text(
                        text = success,
                        color = Color.Green,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Submit button
                Button(
                    onClick = {
                        if (isLogin) {
                            viewModel.login(email.text, password.text, rememberMe, context)
                        } else {
                            viewModel.register(name.text, email.text, password.text, selectedRole)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isLogin) "Login" else "Register")
                }
            }
        }
    }
}

// Main Screen with Navigation
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: AttendanceViewModel, uiState: AttendanceUiState) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance System") },
                actions = {
                    Text(
                        text = "Hello, ${uiState.currentUser?.name}",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    IconButton(onClick = { viewModel.logout(context) }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val tabs = when (uiState.currentUser?.role) {
                    UserRole.STUDENT -> listOf("Dashboard", "Courses", "My Attendance")
                    UserRole.INSTRUCTOR -> listOf("Dashboard", "My Courses", "Manage Courses")
                    null -> emptyList()
                }

                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                when (index) {
                                    0 -> Icons.Default.Dashboard
                                    1 -> Icons.Default.Book
                                    2 -> if (uiState.currentUser?.role == UserRole.STUDENT)
                                        Icons.Default.CheckCircle else Icons.Default.Add
                                    else -> Icons.Default.Home
                                },
                                contentDescription = title
                            )
                        },
                        label = { Text(title) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Messages
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            uiState.success?.let { success ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Green.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = success,
                        modifier = Modifier.padding(16.dp),
                        color = Color.Green
                    )
                }
            }

            // Content based on selected tab
            when (selectedTab) {
                0 -> DashboardScreen(viewModel, uiState)
                1 -> CoursesScreen(viewModel, uiState)
                2 -> if (uiState.currentUser?.role == UserRole.STUDENT) {
                    AttendanceScreen(viewModel, uiState)
                } else {
                    ManageCoursesScreen(viewModel, uiState)
                }
            }
        }
    }
}

// Dashboard Screen
@Composable
fun DashboardScreen(viewModel: AttendanceViewModel, uiState: AttendanceUiState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Overview",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatCard("Courses", uiState.courses.size.toString())

                        if (uiState.currentUser?.role == UserRole.STUDENT) {
                            val todayRecords = uiState.attendanceRecords.filter { record ->
                                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                record.studentId == uiState.currentUser.id && record.date == today && record.isPresent
                            }
                            StatCard("Present Today", todayRecords.size.toString())
                        } else {
                            StatCard("Students", uiState.students.size.toString())
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Recent Activity",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        items(uiState.courses.take(3)) { course ->
            CourseCard(course = course, viewModel = viewModel, uiState = uiState)
        }
    }
}

@Composable
fun StatCard(title: String, value: String) {
    Card(
        modifier = Modifier.width(100.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// Courses Screen
@Composable
fun CoursesScreen(viewModel: AttendanceViewModel, uiState: AttendanceUiState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = if (uiState.currentUser?.role == UserRole.STUDENT) "My Courses" else "My Courses",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(uiState.courses) { course ->
            CourseCard(course = course, viewModel = viewModel, uiState = uiState)
        }

        if (uiState.courses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Book,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No courses available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CourseCard(course: Course, viewModel: AttendanceViewModel, uiState: AttendanceUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = course.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = course.code,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (course.description.isNotEmpty()) {
                        Text(
                            text = course.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Schedule: ${course.schedule}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Attendance buttons for students
                if (uiState.currentUser?.role == UserRole.STUDENT) {
                    val todayAttendance = viewModel.getTodayAttendance(course.id, uiState.currentUser.id)

                    Column(horizontalAlignment = Alignment.End) {
                        todayAttendance?.let { record ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (record.isPresent) Color.Green.copy(alpha = 0.1f)
                                    else Color.Red.copy(alpha = 0.1f)
                                )
                            ) {
                                Text(
                                    text = if (record.isPresent) "Present" else "Absent",
                                    modifier = Modifier.padding(8.dp),
                                    color = if (record.isPresent) Color.Green else Color.Red,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        } ?: run {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = { viewModel.markAttendance(course.id, true) },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = Color.Green.copy(alpha = 0.1f)
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Present",
                                        tint = Color.Green
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.markAttendance(course.id, false) },
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = Color.Red.copy(alpha = 0.1f)
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Cancel,
                                        contentDescription = "Absent",
                                        tint = Color.Red
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Attendance Screen (for students)
@Composable
fun AttendanceScreen(viewModel: AttendanceViewModel, uiState: AttendanceUiState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "My Attendance",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(uiState.courses) { course ->
            AttendanceDetailCard(course = course, viewModel = viewModel, uiState = uiState)
        }

        if (uiState.courses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No attendance records",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceDetailCard(course: Course, viewModel: AttendanceViewModel, uiState: AttendanceUiState) {
    val studentAttendance = uiState.attendanceRecords.filter {
        it.courseId == course.id && it.studentId == uiState.currentUser?.id
    }
    val presentCount = studentAttendance.count { it.isPresent }
    val totalSessions = studentAttendance.size
    val attendancePercentage = if (totalSessions > 0) (presentCount * 100) / totalSessions else 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = course.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = course.code,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Present: $presentCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Green
                    )
                    Text(
                        text = "Absent: ${totalSessions - presentCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Red
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            attendancePercentage >= 75 -> Color.Green.copy(alpha = 0.1f)
                            attendancePercentage >= 50 -> Color.Yellow.copy(alpha = 0.1f)
                            else -> Color.Red.copy(alpha = 0.1f)
                        }
                    )
                ) {
                    Text(
                        text = "$attendancePercentage%",
                        modifier = Modifier.padding(8.dp),
                        color = when {
                            attendancePercentage >= 75 -> Color.Green.copy(alpha = 0.1f)
                            attendancePercentage >= 50 -> Color(0xFFFFA500).copy(alpha = 0.1f)
                            else -> Color.Red.copy(alpha = 0.1f)
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Manage Courses Screen (for instructors)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCoursesScreen(viewModel: AttendanceViewModel, uiState: AttendanceUiState) {
    var showAddCourseDialog by remember { mutableStateOf(false) }
    var courseName by remember { mutableStateOf("") }
    var courseCode by remember { mutableStateOf("") }
    var courseDescription by remember { mutableStateOf("") }
    var courseSchedule by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Manage Courses",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                FloatingActionButton(
                    onClick = { showAddCourseDialog = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Course")
                }
            }
        }

        items(uiState.courses) { course ->
            InstructorCourseCard(course = course, viewModel = viewModel, uiState = uiState)
        }

        if (uiState.courses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No courses created yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap the + button to add your first course",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Add Course Dialog
    if (showAddCourseDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddCourseDialog = false
                courseName = ""
                courseCode = ""
                courseDescription = ""
                courseSchedule = ""
            },
            title = { Text("Add New Course") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = courseName,
                        onValueChange = { courseName = it },
                        label = { Text("Course Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = courseCode,
                        onValueChange = { courseCode = it },
                        label = { Text("Course Code") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = courseDescription,
                        onValueChange = { courseDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    OutlinedTextField(
                        value = courseSchedule,
                        onValueChange = { courseSchedule = it },
                        label = { Text("Schedule") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g., MWF 10:00-11:00") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addCourse(courseName, courseCode, courseDescription, courseSchedule)
                        showAddCourseDialog = false
                        courseName = ""
                        courseCode = ""
                        courseDescription = ""
                        courseSchedule = ""
                    },
                    enabled = courseName.isNotEmpty() && courseCode.isNotEmpty()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddCourseDialog = false
                        courseName = ""
                        courseCode = ""
                        courseDescription = ""
                        courseSchedule = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun InstructorCourseCard(course: Course, viewModel: AttendanceViewModel, uiState: AttendanceUiState) {
    val enrolledStudents = uiState.students.filter { it.id in course.studentIds }
    val todayAttendance = uiState.attendanceRecords.filter { record ->
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        record.courseId == course.id && record.date == today
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = course.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = course.code,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (course.description.isNotEmpty()) {
                Text(
                    text = course.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Schedule: ${course.schedule}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Enrolled: ${enrolledStudents.size}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Present Today: ${todayAttendance.count { it.isPresent }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Green
                    )
                }

                // Today's attendance summary
                if (todayAttendance.isNotEmpty()) {
                    val presentCount = todayAttendance.count { it.isPresent }
                    val totalCount = todayAttendance.size
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = "$presentCount/$totalCount",
                            modifier = Modifier.padding(8.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Student list
            if (enrolledStudents.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Students:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                enrolledStudents.forEach { student ->
                    val studentAttendance = todayAttendance.find { it.studentId == student.id }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = student.name,
                            style = MaterialTheme.typography.bodySmall
                        )
                        studentAttendance?.let { record ->
                            Icon(
                                if (record.isPresent) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = if (record.isPresent) "Present" else "Absent",
                                tint = if (record.isPresent) Color.Green else Color.Red,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}