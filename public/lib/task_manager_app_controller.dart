library task_manager_controller;

import 'package:angular/angular.dart';
import 'package:json_object/json_object.dart';
import 'dart:html';
import 'dart:convert';
import 'dart:core';

@NgController(selector: '[task-manager-app]', publishAs: 'ctrl')
class TaskManagerAppController {
  
  List<TaskTree> tasks = [];
  
  TaskManagerAppController() {
    
    WebSocket ws = new WebSocket('ws://localhost:9000/ws/taskManager');
    
    ws.onMessage.listen((MessageEvent e) {
      
      // Using JsonObject instead of plain Json makes it possible to enforce types checking. 
      // For example, if you try to access a property on this jsonObject that was not originally in it
      // it will throw NoSuchMethodError instead of returning null. 
      JsonObject tasksAsJsonArray = new JsonObject.fromJsonString(e.data);
      
      List<TaskTree> tasks = [];
      
      tasksAsJsonArray.forEach((taskJson) {
        tasks.add(new TaskTree.create(taskJson));
      });
      
      this.tasks = tasks;

    });
    
    ws.onOpen.first.then((_) {
      ws.send(JSON.encode({ 'messageType': 'GetAll' }));  
    });
    
  }
  
}

class Task {
  
  String desc;

  Task(this.desc);

  factory Task.create(JsonObject json) {
    return new Task(json.desc);  
  }
  
}

class TaskTree {
  
  Task task;
  List<TaskTree> subTasks;
  
  TaskTree(this.task, this.subTasks);
  
  factory TaskTree.create(JsonObject json) {
    Task task = new Task.create(json.task);
    // Could not get `map` to work on JSON array (gives a type error), so using emperative approach for now
    List<TaskTree> subTasks = [];
    json.subTasks.forEach((subTaskJson) {
      subTasks.add(new TaskTree.create(subTaskJson)); 
    });
    return new TaskTree(task, subTasks);
  }

}