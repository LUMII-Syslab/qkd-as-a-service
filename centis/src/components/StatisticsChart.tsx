import Chart, {ChartData, ChartItem} from 'chart.js/auto';
import {useContext, useEffect, useRef, useState} from "react";
import {ConfigContext} from "../utils/config-context";
import {wsConnect, wsSendRequest} from "../utils/promise-ws";
import {decodeGetStateResponse, encodeGetStateRequest, GetStateRequest} from "../utils/get-state-req";

export default function StatisticsChart() {
    const config = useContext(ConfigContext)

    let reservableChartRef = useRef();
    let pressureChartRef = useRef();

    let reservableChartData = useRef({
        labels: [],
        datasets: [{
            label: 'Aija reservable Keys',
            data: [],
            tension: 0.3,
            borderColor: '#27ae60',
            backgroundColor: '#2ecc71'
        },
            {
                label: 'Brencis reservable Keys',
                data: [],
                tension: 0.3,
                borderColor: '#2980b9',
                backgroundColor: '#3498db'
            }]
    } as ChartData<"line", any[], any>);
    let pressureChartData = useRef({
        labels: [], datasets: [
            {label: 'keys added', data: [], tension: 0.3, borderColor: '#16a085', backgroundColor: '#1abc9c'},
            {label: 'keys served', data: [], tension: 0.3, borderColor: '#8e44ad', backgroundColor: '#9b59b6'}]
    } as ChartData<"line", any[], any>);

    let [updating, setUpdating] = useState(true);

    useEffect(() => {
        let reservableChart = new Chart(reservableChartRef.current as ChartItem, {
            type: 'line',
            data: reservableChartData.current,
            options: {
                animation: {duration: 0},
                scales: {
                    x: {
                        ticks: {
                            autoSkip: true,
                            maxTicksLimit: 5,
                        },
                    }
                }
            },
        });
        let pressureChart = new Chart(pressureChartRef.current as ChartItem, {
            type: 'line',
            data: pressureChartData.current,
            options: {
                animation: {duration: 0},
                scales: {
                    x: {
                        ticks: {
                            autoSkip: true,
                            maxTicksLimit: 5,
                        }
                    },
                    y: {
                        type: 'logarithmic'
                    }
                }
            },
        });
        let interval = setInterval(async () => {
            if (!updating) return;
            let aijaWs = await wsConnect(config.aijaEndpoint) as WebSocket
            let brencisWS = await wsConnect(config.brencisEndpoint) as WebSocket

            let encodedRequest = encodeGetStateRequest({cNonce: Math.floor(Math.random() * 100000)} as GetStateRequest)

            let aijaResponse = decodeGetStateResponse(await wsSendRequest(aijaWs, encodedRequest))
            let brencisResponse = decodeGetStateResponse(await wsSendRequest(brencisWS, encodedRequest))

            aijaWs.close()
            brencisWS.close()

            reservableChart.data.labels.push(new Date().toLocaleString('sv'))
            reservableChart.data.datasets[0].data.push(aijaResponse.reservable)
            reservableChart.data.datasets[1].data.push(brencisResponse.reservable)
            if (reservableChart.data.labels.length > 20) {
                reservableChart.data.labels.shift()
                reservableChart.data.datasets[0].data.shift()
                reservableChart.data.datasets[1].data.shift()
            }

            pressureChart.data.labels.push(new Date().toLocaleString('sv'))
            pressureChart.data.datasets[0].data.push(aijaResponse.keysAdded)
            pressureChart.data.datasets[1].data.push(brencisResponse.keysServed)
            if (pressureChart.data.labels.length > 20) {
                pressureChart.data.labels.shift()
                pressureChart.data.datasets[0].data.shift()
                pressureChart.data.datasets[1].data.shift()
            }

            reservableChart.update()
            pressureChart.update()
        }, 500)
        return () => {
            clearInterval(interval)
            reservableChartData.current = reservableChart.data
            pressureChartData.current = pressureChart.data
            reservableChart.destroy();
            pressureChart.destroy();
        }
    }, [config.aijaEndpoint, config.brencisEndpoint, updating])

    return (
        <div className="my-4 d-flex flex-wrap h-25 form-control rounded-0 shadow-sm p-3">
            <div className="col-12 col-lg-6 my-1">
                <canvas ref={reservableChartRef}></canvas>
            </div>
            <div className="col-12 col-lg-6 my-1">
                <canvas ref={pressureChartRef}></canvas>
            </div>
            <div className="d-flex flex-row justify-content-end w-100 my-3">
                <button className="btn btn-sm btn-outline-primary"
                        onClick={() => setUpdating(!updating)}>{(updating) ? ("stop updating") : ("start updating")}</button>
            </div>
        </div>
    )
}