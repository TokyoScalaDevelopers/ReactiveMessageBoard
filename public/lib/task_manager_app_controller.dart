library task_manager_controller;

import 'package:angular/angular.dart';

@NgController(
    selector: '[task-manager-app]',
    publishAs: 'ctrl')
class TaskManagerAppController {
  
  String greeting = 'initial value';
  
  void sayHello() {
    greeting = 'Hello world XXX';   
  }
  
  TaskManagerAppController() {
    greeting =  greeting + '!!';
  }
  
}