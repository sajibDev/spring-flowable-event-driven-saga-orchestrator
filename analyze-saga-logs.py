#!/usr/bin/env python3
"""
Saga Timing Log Analyzer
Reads saga-timing.log and generates comprehensive summary reports
"""

import re
import json
from datetime import datetime
from collections import defaultdict
from pathlib import Path
import statistics


class SagaAnalyzer:
    def __init__(self, log_file_path="saga-timing-logs/saga-timing.log"):
        self.log_file = Path(log_file_path)
        self.sagas = {}
        self.events = defaultdict(list)
        
    def parse_logs(self):
        """Parse the saga timing log file"""
        if not self.log_file.exists():
            print(f"❌ Log file not found: {self.log_file}")
            return False
            
        with open(self.log_file, 'r') as f:
            for line in f:
                line = line.strip()
                if not line or line.startswith('='):
                    continue
                    
                # Parse START entries
                if '[START]' in line:
                    self._parse_start(line)
                # Parse EVENT entries
                elif '[EVENT]' in line:
                    self._parse_event(line)
                # Parse COMPLETE entries
                elif '[COMPLETE]' in line:
                    self._parse_complete(line)
                    
        return True
    
    def _parse_start(self, line):
        """Parse saga start entry"""
        match = re.search(r'CorrelationId: ([^\s|]+)', line)
        if match:
            corr_id = match.group(1)
            
            # Extract timestamp
            ts_match = re.search(r'Timestamp: (\d+)', line)
            timestamp = int(ts_match.group(1)) if ts_match else 0
            
            # Extract customer ID
            cust_match = re.search(r'CustomerId: ([^\s|]+)', line)
            customer_id = cust_match.group(1) if cust_match else "UNKNOWN"
            
            self.sagas[corr_id] = {
                'correlationId': corr_id,
                'customerId': customer_id,
                'startTime': timestamp,
                'endTime': None,
                'duration': None,
                'status': 'IN_PROGRESS',
                'events': []
            }
    
    def _parse_event(self, line):
        """Parse saga event entry"""
        corr_match = re.search(r'CorrelationId: ([^\s|]+)', line)
        event_match = re.search(r'EventName: ([^\s|]+)', line)
        success_match = re.search(r'Success: (true|false)', line)
        elapsed_match = re.search(r'ElapsedMs: (\d+)', line)
        
        if corr_match and event_match:
            corr_id = corr_match.group(1)
            event_name = event_match.group(1)
            success = success_match.group(1) == 'true' if success_match else True
            elapsed = int(elapsed_match.group(1)) if elapsed_match else 0
            
            if corr_id in self.sagas:
                self.sagas[corr_id]['events'].append({
                    'name': event_name,
                    'success': success,
                    'elapsed': elapsed
                })
    
    def _parse_complete(self, line):
        """Parse saga completion entry"""
        corr_match = re.search(r'CorrelationId: ([^\s|]+)', line)
        success_match = re.search(r'Success: (true|false)', line)
        duration_match = re.search(r'DurationMs: (\d+)', line)
        ts_match = re.search(r'Timestamp: (\d+)', line)
        
        if corr_match:
            corr_id = corr_match.group(1)
            
            if corr_id in self.sagas:
                success = success_match.group(1) == 'true' if success_match else False
                duration = int(duration_match.group(1)) if duration_match else 0
                
                self.sagas[corr_id]['status'] = 'SUCCESS' if success else 'FAILED'
                self.sagas[corr_id]['duration'] = duration
                self.sagas[corr_id]['endTime'] = int(ts_match.group(1)) if ts_match else 0
            else:
                # Saga completed without start record (edge case)
                print(f"⚠️  Warning: Completion found without start for {corr_id}")
    
    def generate_summary_report(self):
        """Generate comprehensive summary report"""
        total_sagas = len(self.sagas)
        if total_sagas == 0:
            return "No saga data found in logs."
        
        # Count statuses
        completed_sagas = [s for s in self.sagas.values() if s['status'] != 'IN_PROGRESS']
        success_count = len([s for s in completed_sagas if s['status'] == 'SUCCESS'])
        failed_count = len([s for s in completed_sagas if s['status'] == 'FAILED'])
        in_progress = total_sagas - len(completed_sagas)
        
        # Calculate duration statistics
        durations = [s['duration'] for s in completed_sagas if s['duration'] is not None]
        
        report = []
        report.append("╔" + "═" * 98 + "╗")
        report.append("║" + "SAGA EXECUTION SUMMARY REPORT".center(98) + "║")
        report.append("╠" + "═" * 98 + "╣")
        report.append("")
        
        # Overall Statistics
        report.append("║" + "OVERALL STATISTICS".center(98) + "║")
        report.append("╠" + "─" * 98 + "╣")
        report.append(f"║  {'Total Sagas Tracked':<35} │ {total_sagas:>58} ║")
        report.append(f"║  {'Completed Sagas':<35} │ {len(completed_sagas):>58} ║")
        report.append(f"║  {'  - Successful':<35} │ {success_count:>50} ({(success_count*100/len(completed_sagas) if completed_sagas else 0):.1f}%) ║")
        report.append(f"║  {'  - Failed':<35} │ {failed_count:>50} ({(failed_count*100/len(completed_sagas) if completed_sagas else 0):.1f}%) ║")
        report.append(f"║  {'In Progress':<35} │ {in_progress:>58} ║")
        report.append("╠" + "─" * 98 + "╣")
        report.append("")
        
        # Duration Metrics
        if durations:
            report.append("║" + "DURATION METRICS".center(98) + "║")
            report.append("╠" + "─" * 98 + "╣")
            report.append(f"║  {'Metric':<35} │ {'Milliseconds':>20} │ {'Seconds':>20} ║")
            report.append("╠" + "─" * 98 + "╣")
            report.append(f"║  {'Minimum Duration':<35} │ {min(durations):>20} │ {min(durations)/1000:>20.3f} ║")
            report.append(f"║  {'Maximum Duration':<35} │ {max(durations):>20} │ {max(durations)/1000:>20.3f} ║")
            report.append(f"║  {'Average Duration':<35} │ {statistics.mean(durations):>20.2f} │ {statistics.mean(durations)/1000:>20.3f} ║")
            report.append(f"║  {'Median Duration':<35} │ {statistics.median(durations):>20.2f} │ {statistics.median(durations)/1000:>20.3f} ║")
            
            if len(durations) > 1:
                report.append(f"║  {'Standard Deviation':<35} │ {statistics.stdev(durations):>20.2f} │ {statistics.stdev(durations)/1000:>20.3f} ║")
                
            # Percentiles
            sorted_durations = sorted(durations)
            p50 = sorted_durations[len(sorted_durations) // 2]
            p90 = sorted_durations[int(len(sorted_durations) * 0.90)]
            p95 = sorted_durations[int(len(sorted_durations) * 0.95)]
            p99 = sorted_durations[int(len(sorted_durations) * 0.99)] if len(sorted_durations) > 100 else sorted_durations[-1]
            
            report.append("╠" + "─" * 98 + "╣")
            report.append(f"║  {'50th Percentile (P50)':<35} │ {p50:>20} │ {p50/1000:>20.3f} ║")
            report.append(f"║  {'90th Percentile (P90)':<35} │ {p90:>20} │ {p90/1000:>20.3f} ║")
            report.append(f"║  {'95th Percentile (P95)':<35} │ {p95:>20} │ {p95/1000:>20.3f} ║")
            report.append(f"║  {'99th Percentile (P99)':<35} │ {p99:>20} │ {p99/1000:>20.3f} ║")
            report.append("╠" + "─" * 98 + "╣")
        
        report.append("")
        report.append("╚" + "═" * 98 + "╝")
        
        return "\n".join(report)
    
    def generate_detailed_report(self, limit=10):
        """Generate detailed report for individual sagas"""
        report = []
        report.append("\n" + "╔" + "═" * 98 + "╗")
        report.append("║" + "DETAILED SAGA REPORTS".center(98) + "║")
        report.append("╚" + "═" * 98 + "╝")
        report.append("")
        
        # Get completed sagas sorted by duration
        completed = [s for s in self.sagas.values() if s['status'] != 'IN_PROGRESS']
        completed_sorted = sorted(completed, key=lambda x: x['duration'] or 0, reverse=True)
        
        for idx, saga in enumerate(completed_sorted[:limit], 1):
            report.append(f"\n┌{'─' * 98}┐")
            report.append(f"│ Saga #{idx:<92}│")
            report.append(f"├{'─' * 98}┤")
            report.append(f"│  {'Correlation ID':<20}: {saga['correlationId']:<74}│")
            report.append(f"│  {'Customer ID':<20}: {saga['customerId']:<74}│")
            report.append(f"│  {'Status':<20}: {saga['status']:<74}│")
            report.append(f"│  {'Duration':<20}: {saga['duration']} ms ({saga['duration']/1000:.3f} seconds){'':<45}│")
            
            # Event timeline
            if saga['events']:
                report.append(f"├{'─' * 98}┤")
                report.append(f"│  {'Event Timeline':<96}│")
                report.append(f"├{'─' * 98}┤")
                report.append(f"│  {'#':<5} │ {'Event Name':<40} │ {'Status':<10} │ {'Elapsed (ms)':<15} │")
                report.append(f"├{'─' * 98}┤")
                
                for e_idx, event in enumerate(saga['events'], 1):
                    status_icon = "✓" if event['success'] else "✗"
                    report.append(f"│  {e_idx:<5} │ {event['name']:<40} │ {status_icon:<10} │ {event['elapsed']:<15} │")
            
            report.append(f"└{'─' * 98}┘")
        
        if len(completed_sorted) > limit:
            report.append(f"\n... and {len(completed_sorted) - limit} more sagas")
        
        return "\n".join(report)
    
    def export_to_json(self, output_file="saga-analysis.json"):
        """Export analysis data to JSON"""
        data = {
            'total_sagas': len(self.sagas),
            'timestamp': datetime.now().isoformat(),
            'sagas': list(self.sagas.values())
        }
        
        with open(output_file, 'w') as f:
            json.dump(data, f, indent=2)
        
        print(f"✓ Exported to {output_file}")


def main():
    import argparse
    
    parser = argparse.ArgumentParser(description='Analyze Saga Timing Logs')
    parser.add_argument('--log-file', default='saga-timing-logs/saga-timing.log', 
                       help='Path to saga timing log file')
    parser.add_argument('--detailed', action='store_true', 
                       help='Show detailed report for individual sagas')
    parser.add_argument('--limit', type=int, default=10, 
                       help='Number of sagas to show in detailed report')
    parser.add_argument('--export-json', action='store_true', 
                       help='Export analysis to JSON file')
    parser.add_argument('--output-dir', default='saga-timing-logs',
                       help='Output directory for reports')
    
    args = parser.parse_args()
    
    # Create analyzer
    analyzer = SagaAnalyzer(args.log_file)
    
    print("📊 Analyzing saga timing logs...")
    print(f"📁 Log file: {args.log_file}")
    print()
    
    # Parse logs
    if not analyzer.parse_logs():
        return 1
    
    # Generate and display summary report
    summary = analyzer.generate_summary_report()
    print(summary)
    
    # Save summary to file
    summary_file = Path(args.output_dir) / "python-summary-report.txt"
    with open(summary_file, 'w') as f:
        f.write(summary)
    print(f"\n✓ Summary saved to: {summary_file}")
    
    # Detailed report if requested
    if args.detailed:
        detailed = analyzer.generate_detailed_report(args.limit)
        print(detailed)
        
        detailed_file = Path(args.output_dir) / "python-detailed-report.txt"
        with open(detailed_file, 'w') as f:
            f.write(detailed)
        print(f"\n✓ Detailed report saved to: {detailed_file}")
    
    # Export to JSON if requested
    if args.export_json:
        json_file = Path(args.output_dir) / "saga-analysis.json"
        analyzer.export_to_json(json_file)
    
    return 0


if __name__ == '__main__':
    exit(main())
