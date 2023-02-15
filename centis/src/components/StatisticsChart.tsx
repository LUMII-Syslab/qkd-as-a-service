import Chart, {ChartItem} from 'chart.js/auto';
import {useEffect, useRef} from "react";

export default function StatisticsChart() {
    let chartCanvas = useRef();

    useEffect(() => {
        let chart = new Chart(chartCanvas.current as ChartItem, {
            type: 'line',
            data: {
                labels: ['1', '2', '3'],
                datasets: [{
                    label: 'reservable keys',
                    data: [12, 19, 3],
                    tension: 0.1,
                }]
            }
        });
        return () => {
            chart.destroy();
        }
    },[])

    return (
        <div>
            <canvas ref={chartCanvas}></canvas>
        </div>
    )
}