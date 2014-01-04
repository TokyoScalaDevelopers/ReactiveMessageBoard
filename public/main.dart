library task_manager_app;

import 'package:angular/angular.dart';

import 'package:ReactiveBlog/task_manager_app_controller.dart';
import 'package:ReactiveBlog/component/task_tree_component.dart';

class TaskManagerApp extends Module {
  TaskManagerApp() {
    type(TaskManagerAppController);
    type(TaskTreeComponent);
  }
}

main() {
  ngBootstrap(module: new TaskManagerApp());
}