library task_manager_controller;

import 'package:angular/angular.dart';
import 'dart:html';
import 'dart:convert';
import 'dart:core';

@NgController(selector: '[task-manager-app]', publishAs: 'ctrl')
class TaskManagerAppController {
  
  List<String> messages = ["test message"];
  
  WebSocket ws;
  
  TaskManagerAppController() {
    
    this.ws = new WebSocket('ws://localhost:9000/ws/taskManager');
    
    this.ws.onMessage.listen((MessageEvent e) {
      var json = JSON.decode(e.data);
      messages.add(json['messageType']);  
    });
    
  }
  
  String textBoxValue = 'xxx';
  
  void sendMessage() {
    ws.send(JSON.encode({ 'messageType': 'GetAll', 'value': this.textBoxValue }));
    this.textBoxValue = '';
  }
  
}