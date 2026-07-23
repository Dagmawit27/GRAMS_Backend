import 'package:flutter/material.dart';

void main() {
  runApp(const EndrmsMobileApp());
}

class EndrmsMobileApp extends StatelessWidget {
  const EndrmsMobileApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'ENDRMS',
      home: const Scaffold(body: Center(child: Text('ENDRMS Mobile'))),
    );
  }
}
