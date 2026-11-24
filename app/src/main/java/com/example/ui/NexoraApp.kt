package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.SupervisorAccount
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppPage
import com.example.data.NexoraHelpers
import com.example.data.UserRole
import com.example.ui.components.NexoraCard
import com.example.ui.components.NexoraLogoBadge
import com.example.ui.components.NexoraNoticeBox
import com.example.ui.components.NexoraPrimaryButton
import com.example.ui.screens.AnnouncementsScreen
import com.example.ui.screens.AttendanceReportsScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.ClassbookScreen
import com.example.ui.screens.ClassesScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DirectorsScreen
import com.example.ui.screens.HomeworkScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MessagesScreen
import com.example.ui.screens.ReportProblemScreen
import com.example.ui.screens.ReportedProblemsScreen
import com.example.ui.screens.SchoolsScreen
import com.example.ui.screens.TeachersScreen
import com.example.ui.screens.TimetableScreen
import com.example.ui.screens.UpdatesScreen
import com.example.ui.theme.NexoraBg
import com.example.ui.theme.NexoraBlueBright
import com.example.ui.theme.NexoraBlueLight
import com.example.ui.theme.NexoraCard
import com.example.ui.theme.NexoraCardElevated
import com.example.ui.theme.NexoraLine
import com.example.ui.theme.NexoraMuted
import com.example.ui.theme.NexoraText
import com.example.viewmodel.NexoraUiState
import com.example.viewmodel.NexoraViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NexoraApp(viewModel: NexoraViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.toastMessage) {
        if (state.toastMessage != null) {
            delay(3200L)
            viewModel.dismissToast()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(NexoraBg)) {
        when {
            state.isInitializing -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        NexoraLogoBadge(size = 56.dp)
                        CircularProgressIndicator(color = NexoraBlueBright)
                        Text("Verbinde mit Supabase...", style = MaterialTheme.typography.bodyMedium, color = NexoraMuted)
                    }
                }
            }

            state.initError != null && state.session == null && state.data.schools.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    NexoraCard(modifier = Modifier.widthIn(max = 420.dp)) {
                        Text("Verbindungsfehler", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        NexoraNoticeBox(state.initError.orEmpty(), isError = true)
                        Spacer(Modifier.height(16.dp))
                        NexoraPrimaryButton(text = "Erneut versuchen", onClick = { viewModel.initializeApp() })
                    }
                }
            }

            state.session == null -> {
                LoginScreen(
                    schools = state.data.schools,
                    updates = state.data.updates,
                    isBusy = state.isBusy,
                    onLoadUpdates = { viewModel.loadUpdates() },
                    onLoginSchoolUser = { sid, user, pass -> viewModel.loginSchoolUser(sid, user, pass) },
                    onLoginAdmin = { email, pass, onSuccess -> viewModel.loginAdmin(email, pass, onSuccess) },
                    onLoginDirector = { name, pass, onSuccess -> viewModel.loginDirector(name, pass, onSuccess) }
                )
            }

            else -> {
                AuthenticatedAppShell(state = state, viewModel = viewModel)
            }
        }

        // Toast overlay
        AnimatedVisibility(
            visible = state.toastMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 84.dp)
        ) {
            Surface(
                color = NexoraCardElevated,
                contentColor = Color.White,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, NexoraBlueLight.copy(alpha = 0.4f)),
                shadowElevation = 10.dp
            ) {
                Text(
                    text = state.toastMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedAppShell(
    state: NexoraUiState,
    viewModel: NexoraViewModel
) {
    val session = state.session ?: return
    val allowedPages = NexoraHelpers.allowedPagesFor(session.type)
    val defaultPage = NexoraHelpers.defaultPageFor(session.type)

    BackHandler(enabled = state.currentPage != defaultPage) {
        viewModel.navigateTo(defaultPage)
    }

    val school = state.data.schools.find { it.id == session.schoolId }
    val classObj = school?.classes?.find { it.id == session.classId }
    val teacherObj = state.data.teachers.find { it.id == session.teacherId }

    val rolePill = when (session.type) {
        UserRole.ADMIN -> "Administrator"
        UserRole.DIRECTOR -> "${school?.name.orEmpty()} · Direktor (${session.name})"
        UserRole.TEACHER -> "${school?.name.orEmpty()} · ${teacherObj?.name ?: session.teacherName}"
        UserRole.CLASS -> "${school?.name.orEmpty()} · Klasse ${classObj?.name.orEmpty()}"
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 720.dp

        val drawerContent: @Composable () -> Unit = {
            ModalDrawerSheet(
                drawerContainerColor = NexoraCard,
                drawerContentColor = NexoraText,
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NexoraLogoBadge(size = 32.dp)
                    Column {
                        Text("Nexora", style = MaterialTheme.typography.titleLarge, color = Color.White)
                        Text(rolePill, style = MaterialTheme.typography.bodySmall, color = NexoraMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                HorizontalDivider(color = NexoraLine)
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp)
                ) {
                    allowedPages.forEach { page ->
                        NavigationDrawerItem(
                            selected = state.currentPage == page,
                            onClick = {
                                scope.launch { drawerState.close() }
                                viewModel.navigateTo(page)
                            },
                            icon = { Icon(iconForPage(page), contentDescription = page.label) },
                            label = { Text(page.label) },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = NexoraBlueBright.copy(alpha = 0.18f),
                                selectedIconColor = NexoraBlueLight,
                                selectedTextColor = NexoraBlueLight,
                                unselectedIconColor = NexoraMuted,
                                unselectedTextColor = NexoraMuted
                            ),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
                HorizontalDivider(color = NexoraLine)
                NavigationDrawerItem(
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.logout()
                    },
                    icon = { Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Abmelden") },
                    label = { Text("Abmelden") },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedIconColor = NexoraMuted,
                        unselectedTextColor = NexoraMuted
                    ),
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = !isWide,
            drawerContent = drawerContent
        ) {
            Scaffold(
                containerColor = NexoraBg,
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    Column {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = NexoraCard.copy(alpha = 0.95f),
                                titleContentColor = Color.White
                            ),
                            navigationIcon = {
                                if (!isWide) {
                                    IconButton(
                                        onClick = { scope.launch { drawerState.open() } },
                                        modifier = Modifier.testTag("open_drawer_button")
                                    ) {
                                        Icon(Icons.Rounded.Menu, contentDescription = "Menü öffnen", tint = NexoraText)
                                    }
                                }
                            },
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    NexoraLogoBadge(size = 28.dp)
                                    Text(
                                        text = buildAnnotatedString {
                                            withStyle(SpanStyle(color = NexoraBlueBright, fontWeight = FontWeight.ExtraBold)) {
                                                append("Nexora")
                                            }
                                            withStyle(SpanStyle(color = NexoraText, fontWeight = FontWeight.ExtraBold)) {
                                                append(".")
                                            }
                                        },
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Surface(
                                        color = NexoraCardElevated,
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(1.dp, NexoraLine),
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Text(
                                            text = rolePill,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NexoraMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { viewModel.syncNow(silent = false) },
                                    modifier = Modifier.testTag("sync_button")
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = "Aktualisieren", tint = NexoraBlueLight)
                                }
                                IconButton(
                                    onClick = { viewModel.logout() },
                                    modifier = Modifier.testTag("logout_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Abmelden", tint = NexoraMuted)
                                }
                            }
                        )
                        if (state.isBusy || state.isSyncing) {
                            LinearProgressIndicator(
                                color = NexoraBlueBright,
                                trackColor = NexoraLine,
                                modifier = Modifier.fillMaxWidth().height(2.dp)
                            )
                        }
                    }
                },
                bottomBar = {
                    if (!isWide) {
                        val quickPages = allowedPages.take(4)
                        val showingMoreSelected = !quickPages.contains(state.currentPage)
                        NavigationBar(
                            containerColor = NexoraCard.copy(alpha = 0.96f),
                            tonalElevation = 0.dp
                        ) {
                            quickPages.forEach { page ->
                                val selected = state.currentPage == page
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.navigateTo(page) },
                                    icon = { Icon(iconForPage(page), contentDescription = page.label) },
                                    label = {
                                        Text(
                                            shortLabelFor(page),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = NexoraBlueLight,
                                        selectedTextColor = NexoraBlueLight,
                                        indicatorColor = NexoraBlueBright.copy(alpha = 0.18f),
                                        unselectedIconColor = NexoraMuted,
                                        unselectedTextColor = NexoraMuted
                                    )
                                )
                            }
                            if (allowedPages.size > 4) {
                                NavigationBarItem(
                                    selected = showingMoreSelected,
                                    onClick = { scope.launch { drawerState.open() } },
                                    icon = { Icon(Icons.Rounded.MoreHoriz, contentDescription = "Mehr") },
                                    label = { Text("Mehr") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = NexoraBlueLight,
                                        selectedTextColor = NexoraBlueLight,
                                        indicatorColor = NexoraBlueBright.copy(alpha = 0.18f),
                                        unselectedIconColor = NexoraMuted,
                                        unselectedTextColor = NexoraMuted
                                    )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isWide) {
                        NavigationRail(
                            containerColor = NexoraCard.copy(alpha = 0.9f),
                            modifier = Modifier
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Spacer(Modifier.height(8.dp))
                            allowedPages.forEach { page ->
                                NavigationRailItem(
                                    selected = state.currentPage == page,
                                    onClick = { viewModel.navigateTo(page) },
                                    icon = { Icon(iconForPage(page), contentDescription = page.label) },
                                    label = { Text(shortLabelFor(page), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = NexoraBlueLight,
                                        selectedTextColor = NexoraBlueLight,
                                        indicatorColor = NexoraBlueBright.copy(alpha = 0.18f),
                                        unselectedIconColor = NexoraMuted,
                                        unselectedTextColor = NexoraMuted
                                    )
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(modifier = Modifier.fillMaxSize().widthIn(max = 1200.dp)) {
                            RenderCurrentPage(state = state, viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderCurrentPage(
    state: NexoraUiState,
    viewModel: NexoraViewModel
) {
    val session = state.session ?: return
    when (state.currentPage) {
        AppPage.DASHBOARD, AppPage.DIRECTOR_DASHBOARD -> DashboardScreen(
            session = session,
            data = state.data
        )

        AppPage.TIMETABLE, AppPage.DIRECTOR_TIMETABLE, AppPage.TEACHER_LESSONS -> TimetableScreen(
            session = session,
            data = state.data,
            weekOffset = state.weekOffset,
            selectedSchoolId = state.selectedSchoolId,
            selectedClassId = state.selectedClassId,
            isTeacherHomeworkMode = state.currentPage == AppPage.TEACHER_LESSONS,
            onSelectSchool = { viewModel.selectSchool(it) },
            onSelectClass = { viewModel.selectClass(it) },
            onChangeWeek = { viewModel.changeWeek(it) },
            onModalOpenChanged = { viewModel.setModalOpen(it) },
            onSaveTeacherLesson = { lid, week, isExam, hwText, hwDate, onDone ->
                viewModel.saveTeacherLesson(lid, week, isExam, hwText, hwDate, onDone)
            },
            onSaveAdminLesson = { sid, lid, cid, day, time, subj, teacher, room, color, isExam, week, status, subT, subR, hwText, hwDate, existingHwId, existingOv, onDone ->
                viewModel.saveAdminOrDirectorLesson(
                    sid, lid, cid, day, time, subj, teacher, room, color, isExam, week,
                    status, subT, subR, hwText, hwDate, existingHwId, existingOv, onDone
                )
            },
            onDeleteLesson = { lid, onDone -> viewModel.deleteLesson(lid, onDone) }
        )

        AppPage.SCHOOLS -> SchoolsScreen(
            data = state.data,
            onModalOpenChanged = { viewModel.setModalOpen(it) },
            onSaveSchool = { id, name, city, onDone -> viewModel.saveSchool(id, name, city, onDone) },
            onDeleteSchool = { viewModel.deleteSchool(it) }
        )

        AppPage.CLASSES, AppPage.DIRECTOR_CLASSES -> ClassesScreen(
            session = session,
            data = state.data,
            onModalOpenChanged = { viewModel.setModalOpen(it) },
            onSaveClass = { id, sid, name, user, pass, onDone -> viewModel.saveClass(id, sid, name, user, pass, onDone) },
            onDeleteClass = { viewModel.deleteClass(it) },
            onAddStudent = { sid, cid, name -> viewModel.addStudent(sid, cid, name) },
            onDeleteStudent = { viewModel.deleteStudent(it) }
        )

        AppPage.TEACHERS, AppPage.DIRECTOR_TEACHERS -> TeachersScreen(
            session = session,
            data = state.data,
            onModalOpenChanged = { viewModel.setModalOpen(it) },
            onSaveTeacher = { id, sid, name, user, subs, pass, onDone -> viewModel.saveTeacher(id, sid, name, user, subs, pass, onDone) },
            onDeleteTeacher = { viewModel.deleteTeacher(it) }
        )

        AppPage.DIRECTORS -> DirectorsScreen(
            data = state.data,
            directors = state.directorsList,
            directorsError = state.directorsError,
            onModalOpenChanged = { viewModel.setModalOpen(it) },
            onCreateDirector = { sid, name, pass, onDone -> viewModel.createDirector(sid, name, pass, onDone) },
            onDeleteDirector = { viewModel.deleteDirector(it) }
        )

        AppPage.CLASSBOOK -> ClassbookScreen(
            session = session,
            data = state.data,
            selectedClassId = state.selectedClassId,
            selectedDate = state.classbookDate,
            classbookLoadError = state.classbookLoadError,
            onSelectClass = { viewModel.selectClass(it) },
            onDateChange = { viewModel.setClassbookDate(it) },
            onModalOpenChanged = { viewModel.setModalOpen(it) },
            onToast = { viewModel.showToast(it) },
            onSaveAttendance = { cid, date, timeIdx, rows -> viewModel.saveAttendance(cid, date, timeIdx, rows) },
            onUpdateAttendance = { id, date, timeIdx, status, mins, onDone -> viewModel.updateAttendance(id, date, timeIdx, status, mins, onDone) },
            onDeleteAttendance = { viewModel.deleteAttendance(it) },
            onSaveClassbookEntry = { stId, type, title, desc, date, onDone -> viewModel.saveClassbookEntry(stId, type, title, desc, date, onDone) },
            onUpdateClassbookEntry = { id, title, desc, type, onDone -> viewModel.updateClassbookEntry(id, title, desc, type, onDone) },
            onDeleteClassbookEntry = { viewModel.deleteClassbookEntry(it) },
            onSaveGrade = { cid, st, subj, valStr, onDone -> viewModel.saveGrade(cid, st, subj, valStr, onDone) },
            onDeleteGrade = { viewModel.deleteGrade(it) }
        )

        AppPage.ATTENDANCE_REPORTS -> AttendanceReportsScreen(
            data = state.data,
            onModalOpenChanged = { viewModel.setModalOpen(it) },
            onUpdateAttendance = { id, date, timeIdx, status, mins, onDone -> viewModel.updateAttendance(id, date, timeIdx, status, mins, onDone) },
            onDeleteAttendance = { viewModel.deleteAttendance(it) }
        )

        AppPage.HOMEWORK -> HomeworkScreen(
            session = session,
            data = state.data,
            onModalOpenChanged = { viewModel.setModalOpen(it) },
            onSaveHomework = { title, text, due, cid, onDone -> viewModel.saveHomework(title, text, due, cid, onDone) },
            onDeleteHomework = { viewModel.deleteHomework(it) }
        )

        AppPage.MESSAGES -> MessagesScreen(
            session = session,
            data = state.data,
            onModalOpenChanged = { viewModel.setModalOpen(it) },
            onSaveMessage = { title, text, onDone -> viewModel.saveMessage(title, text, onDone) },
            onDeleteMessage = { viewModel.deleteMessage(it) }
        )

        AppPage.CALENDAR, AppPage.DIRECTOR_CALENDAR -> CalendarScreen(
            session = session,
            data = state.data,
            onModalOpenChanged = { viewModel.setModalOpen(it) },
            onSaveEvent = { sid, title, date, text, onDone -> viewModel.saveCalendarEvent(sid, title, date, text, onDone) },
            onDeleteEvent = { viewModel.deleteCalendarEvent(it) }
        )

        AppPage.ANNOUNCEMENTS -> AnnouncementsScreen(
            session = session,
            data = state.data,
            onModalOpenChanged = { viewModel.setModalOpen(it) },
            onAddAnnouncement = { sid, text, onDone -> viewModel.addAnnouncement(sid, text, onDone) },
            onDeleteAnnouncement = { sid, idx -> viewModel.deleteAnnouncement(sid, idx) }
        )

        AppPage.REPORT_PROBLEM, AppPage.DIRECTOR_REPORT_PROBLEM -> ReportProblemScreen(
            onSubmitProblem = { title, desc, onDone -> viewModel.submitProblemReport(title, desc, onDone) }
        )

        AppPage.REPORTED_PROBLEMS -> ReportedProblemsScreen(
            data = state.data,
            error = state.problemReportsError,
            onRefresh = { viewModel.loadProblemReports() },
            onDeleteReport = { viewModel.deleteProblemReport(it) }
        )

        AppPage.UPDATES, AppPage.UPDATES_ADMIN -> UpdatesScreen(
            data = state.data,
            isAdminManage = state.currentPage == AppPage.UPDATES_ADMIN,
            onPublishUpdate = { title, desc, onDone -> viewModel.publishUpdate(title, desc, onDone) },
            onDeleteUpdate = { viewModel.deleteUpdate(it) }
        )
    }
}

private fun iconForPage(page: AppPage): ImageVector = when (page) {
    AppPage.DASHBOARD -> Icons.Rounded.Dashboard
    AppPage.DIRECTOR_DASHBOARD -> Icons.Rounded.Apartment
    AppPage.TIMETABLE, AppPage.DIRECTOR_TIMETABLE -> Icons.Rounded.CalendarMonth
    AppPage.TEACHER_LESSONS -> Icons.Rounded.EditNote
    AppPage.SCHOOLS -> Icons.Rounded.School
    AppPage.CLASSES, AppPage.DIRECTOR_CLASSES -> Icons.Rounded.Groups
    AppPage.TEACHERS, AppPage.DIRECTOR_TEACHERS -> Icons.Rounded.SupervisorAccount
    AppPage.DIRECTORS -> Icons.Rounded.AdminPanelSettings
    AppPage.CLASSBOOK -> Icons.Rounded.MenuBook
    AppPage.ATTENDANCE_REPORTS -> Icons.Rounded.FactCheck
    AppPage.HOMEWORK -> Icons.Rounded.EditNote
    AppPage.MESSAGES -> Icons.AutoMirrored.Rounded.Message
    AppPage.CALENDAR, AppPage.DIRECTOR_CALENDAR -> Icons.Rounded.Event
    AppPage.ANNOUNCEMENTS -> Icons.Rounded.NotificationsActive
    AppPage.REPORT_PROBLEM, AppPage.DIRECTOR_REPORT_PROBLEM -> Icons.Rounded.Flag
    AppPage.REPORTED_PROBLEMS -> Icons.Rounded.ReportProblem
    AppPage.UPDATES, AppPage.UPDATES_ADMIN -> Icons.Rounded.Campaign
}

private fun shortLabelFor(page: AppPage): String = when (page) {
    AppPage.DIRECTOR_DASHBOARD -> "Schule"
    AppPage.DIRECTOR_CLASSES -> "Klassen"
    AppPage.DIRECTOR_TEACHERS -> "Lehrer"
    AppPage.TEACHER_LESSONS -> "Aufgaben"
    AppPage.ATTENDANCE_REPORTS -> "Fehlzeiten"
    AppPage.CALENDAR -> "Termine"
    AppPage.DIRECTOR_CALENDAR -> "Termine"
    AppPage.ANNOUNCEMENTS -> "Infos"
    AppPage.REPORT_PROBLEM, AppPage.DIRECTOR_REPORT_PROBLEM -> "Problem"
    AppPage.REPORTED_PROBLEMS -> "Probleme"
    AppPage.UPDATES_ADMIN -> "Updates"
    else -> page.label
}
