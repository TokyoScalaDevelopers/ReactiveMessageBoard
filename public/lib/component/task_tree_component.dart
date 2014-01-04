library task_tree_component;

import 'package:angular/angular.dart';
import 'package:ReactiveBlog/task_manager_app_controller.dart';

@NgComponent(
    selector: 'task-tree',
    templateUrl: 'assets/packages/ReactiveBlog/component/task_tree_component.html',
    cssUrl: 'assets/packages/ReactiveBlog/component/task_tree_component.css',
    publishAs: 'ctrl',
    map: const {
      'task-tree':'<=>taskTree'
    }
)
class TaskTreeComponent {
  
  TaskTree taskTree;

}
