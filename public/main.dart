library task_manager_app;

import 'package:angular/angular.dart';

import 'package:ReactiveBlog/task_manager_app_controller.dart';

class TaskManagerApp extends Module {
  TaskManagerApp() {
    type(TaskManagerAppController);
  }
}

main() {
  ngBootstrap(module: new TaskManagerApp());
}