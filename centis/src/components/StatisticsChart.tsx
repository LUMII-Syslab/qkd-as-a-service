import Chart, {ChartItem} from 'chart.js/auto';
import {useContext, useEffect, useRef} from "react";
import {ConfigContext} from "../utils/config-context";
import {wsConnect, wsSendRequest} from "../utils/promise-ws";
import {decodeGetStateResponse, encodeGetStateRequest, GetStateRequest} from "../utils/get-state-req";

export default function StatisticsChart() {
    const config = useContext(ConfigContext)

    let reservableChartRef = useRef();
    let pressureChartRef = useRef();

    useEffect(() => {
        let reservableChart = new Chart(reservableChartRef.current as ChartItem, {
            type: 'line',
            data: {
                labels: [],
                datasets: [{label: 'Aija reservable Keys', data: [], tension: 0.3, pointRadius:0},
                    {label: 'Brencis reservable Keys', data: [], tension: 0.3, pointRadius:0}]
            },
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
            data: {
                labels: [],
                datasets: [{label: 'keys added', data: [], tension: 0.3},
                    {label: 'keys served', data: [], tension: 0.3}]
            },
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
            let aijaWs = await wsConnect(config.aijaEndpoint) as WebSocket
            let brencisWS = await wsConnect(config.brencisEndpoint) as WebSocket

            let encodedRequest = encodeGetStateRequest({cNonce: Math.floor(Math.random() * 100000)} as GetStateRequest)

            let aijaResponse = decodeGetStateResponse(await wsSendRequest(aijaWs, encodedRequest))
            let brencisResponse = decodeGetStateResponse(await wsSendRequest(brencisWS, encodedRequest))

            aijaWs.close()
            brencisWS.close()

            console.log(aijaResponse)
            console.log(brencisResponse)

            reservableChart.data.labels.push(new Date().toLocaleString('sv'))
            reservableChart.data.datasets[0].data.push(aijaResponse.reservable)
            reservableChart.data.datasets[1].data.push(brencisResponse.reservable)

            pressureChart.data.labels.push(new Date().toLocaleString('sv'))
            pressureChart.data.datasets[0].data.push(aijaResponse.keysAdded)
            pressureChart.data.datasets[1].data.push(brencisResponse.keysServed)

            reservableChart.update()
            pressureChart.update()
        }, 500)
        return () => {
            clearInterval(interval)
            reservableChart.destroy();
            pressureChart.destroy();
        }
    }, [])

    return (
        <div className="my-4 d-flex flex-wrap h-25">
            <div className="col-12 col-lg-6 my-1">
                <canvas ref={reservableChartRef}></canvas>
            </div>
            <div className="col-12 col-lg-6 my-1">
                <canvas ref={pressureChartRef}></canvas>
            </div>
        </div>
    )
}